package com.example.SmartPhone.service;

import com.example.SmartPhone.model.Phone;
import com.example.SmartPhone.repository.PhoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phone Catalog Service
 * 
 * This service acts as the main coordinator for phone data access and search operations.
 * It orchestrates multiple specialized services to provide:
 * 
 * 1. Phone data retrieval from database
 * 2. Trending phones calculation (based on price)
 * 3. Intelligent search with spell correction and ranking
 * 4. Search frequency tracking for analytics
 * 
 * Think of this as the "conductor" that brings together all our specialized
 * services to create a seamless search experience.
 */
@Service
public class CsvService {

    private static final Logger log = LoggerFactory.getLogger(CsvService.class);
    
    // Database access layer
    private final PhoneRepository phoneRepository;
    
    // Specialized feature services
    private final FrequencyService frequencyService;    // Tracks search patterns
    private final SpellCheckService spellCheckService;  // Fixes typos and provides suggestions
    private final RankingService rankingService;        // Ranks search results by relevance

    /**
     * Constructor: Spring injects all required dependencies
     */
    public CsvService(PhoneRepository phoneRepository, FrequencyService frequencyService, 
                     SpellCheckService spellCheckService, RankingService rankingService) {
        this.phoneRepository = phoneRepository;
        this.frequencyService = frequencyService;
        this.spellCheckService = spellCheckService;
        this.rankingService = rankingService;
    }

    /**
     * Retrieve All Phones from Database
     * 
     * Originally this service read phones from a CSV file (hence the name CsvService),
     * but we've upgraded to use a PostgreSQL database for better performance and reliability.
     * 
     * @return List of all phones in our catalog, or empty list if error occurs
     */
    public List<Phone> readAllPhones() {
        try {
            log.debug("Fetching all phones from database");
            List<Phone> phones = phoneRepository.findAll();
            log.debug("Successfully fetched {} phones", phones.size());
            return phones;
        } catch (DataAccessException e) {
            log.error("Database error while fetching all phones", e);
            return Collections.emptyList();  // Don't crash - return empty list
        } catch (Exception e) {
            log.error("Unexpected error while fetching all phones", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get Trending Phones
     * 
     * Returns the most expensive phones in our catalog
     * The logic is: Higher price = More premium = More likely to trend
     * 
     * This appears on the dashboard homepage to showcase flagship devices
     * 
     * @param limit How many trending phones to return (e.g., 12 for the homepage)
     * @return List of phones sorted by price (highest first), limited to specified count
     */
    public List<Phone> topTrending(int limit) {
        try {
            // Safety check: Limit must be positive
            if (limit <= 0) {
                log.warn("Invalid limit for topTrending: {}", limit);
                limit = 12; // Default to showing 12 phones
            }
            
            log.debug("Fetching top {} trending phones", limit);
            List<Phone> all = phoneRepository.findAll();
            
            if (all == null || all.isEmpty()) {
                log.info("No phones found in database");
                return Collections.emptyList();
            }
            
            // Sort by price in DESCENDING order (most expensive first)
            // Null-safe: If price is null, treat it as 0
            List<Phone> sorted = all.stream()
                    .sorted(Comparator.comparing(p -> p.getPriceCAD() == null ? 0.0 : -p.getPriceCAD()))
                    .collect(Collectors.toList());
                    
            // Return only the top 'limit' phones
            if (sorted.size() > limit) {
                return sorted.subList(0, limit);
            }
            return sorted;
        } catch (DataAccessException e) {
            log.error("Database error while fetching trending phones", e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error while fetching trending phones", e);
            return Collections.emptyList();
        }
    }

    /**
     * Intelligent Phone Search with Ranking and Spell Correction
     * 
     * This is the heart of our search functionality. It combines multiple advanced features:
     * 
     * 1. SEARCH FREQUENCY TRACKING: Logs every search to identify popular terms
     * 2. SPELL CHECKING: Suggests corrections for typos (e.g., "appel" → "apple")
     * 3. PAGE RANKING: Scores and sorts results by relevance
     * 
     * The ranking algorithm considers:
     * - Exact matches (highest priority)
     * - Fuzzy matches (handles "smasung" → "samsung")
     * - Partial matches (brand or model contains search term)
     * - Brand-level matches (lowest priority)
     * 
     * @param keyword User's search query (may contain typos or partial words)
     * @return List of phones ranked by relevance to the search query
     */
    public List<Phone> search(String keyword) {
        try {
            // Handle empty searches - show all phones
            if (keyword == null || keyword.trim().isEmpty()) {
                log.debug("Empty search keyword, returning all phones");
                return readAllPhones();
            }
            
            String kw = keyword.trim().toLowerCase();
            log.info("Searching for keyword: {}", kw);
            
            // Safety check: Prevent excessively long search queries
            if (kw.length() > 200) {
                log.warn("Search keyword too long: {} characters", kw.length());
                return Collections.emptyList();
            }
            
            // FEATURE: SEARCH FREQUENCY TRACKING
            // Track this search query to identify popular search terms
            try {
                frequencyService.logSearch(kw);
            } catch (Exception e) {
                log.error("Error logging search", e);
                // Continue with search even if logging fails - don't impact user experience
            }
            
            // ═══════════════════════════════════════════════════════════
            // SPELL CHECKING & AUTO-CORRECTION
            // ═══════════════════════════════════════════════════════════
            // Before searching, check if the user might have made a typo
            // Example: "appel" gets auto-corrected to "apple"
            //          "smasung" gets auto-corrected to "samsung"
            
            String correctedQuery = kw;
            boolean isLikelyMisspelling = false;
            try {
                List<String> suggestions = spellCheckService.suggestions(kw, 1);
                
                // If spell checker suggests a different word, it might be a typo
                if (suggestions != null && !suggestions.isEmpty()) {
                    String firstSuggestion = suggestions.get(0).toLowerCase();
                    // Only consider it a correction if it's actually different
                    if (!firstSuggestion.equals(kw)) {
                        isLikelyMisspelling = true;
                        correctedQuery = firstSuggestion;
                        log.debug("Spell suggestion available: {} → {}", kw, correctedQuery);
                    }
                }
            } catch (Exception e) {
                log.error("Error getting spell suggestions", e);
                // Not critical - continue with original query
            }
            
            // ═══════════════════════════════════════════════════════════
            // SEARCH STRATEGY: Try original query first
            // ═══════════════════════════════════════════════════════════
            // Why? Because our fuzzy matching is smart enough to handle typos
            // Example: "Smasung S23" will still find "Samsung Galaxy S23" even with the typo
            
            List<Phone> originalResults = null;
            try {
                originalResults = rankingService.getRankedPhones(kw, 100);
                if (originalResults != null && !originalResults.isEmpty()) {
                    log.info("Found {} phones with original query '{}' (ranked by relevance)", originalResults.size(), kw);
                    return originalResults;
                }
            } catch (Exception e) {
                log.debug("No ranked results for original query: {}", kw);
            }
            
            // ═══════════════════════════════════════════════════════════
            // SPECIAL CASE: Storage-based searches
            // ═══════════════════════════════════════════════════════════
            // If user is searching for storage capacity (e.g., "512gb", "256"),
            // prioritize searching the storage field directly
            
            // FEATURE: REGEX VALIDATION (from RankingService)
            if (correctedQuery.matches(".*\\d+.*") || correctedQuery.contains("gb")) {
                // Extract just the numbers from the query
                // Example: "512 gb" → "512", "256" → "256"
                String digits = correctedQuery.replaceAll("[^0-9]", "");
                
                if (!digits.isEmpty()) {
                    try {
                        List<Phone> byStorageDigits = phoneRepository.findByStorageContainingIgnoreCase(digits);
                        if (byStorageDigits != null && !byStorageDigits.isEmpty()) {
                            log.info("Found {} phones matching storage digits: {}", byStorageDigits.size(), digits);
                            return byStorageDigits;
                        }
                    } catch (DataAccessException e) {
                        log.error("Database error searching by storage digits", e);
                    }
                }
                
                // Also try the full corrected query on storage field
                try {
                    List<Phone> byStorage = phoneRepository.findByStorageContainingIgnoreCase(correctedQuery);
                    if (byStorage != null && !byStorage.isEmpty()) {
                        log.info("Found {} phones matching storage: {}", byStorage.size(), correctedQuery);
                        return byStorage;
                    }
                } catch (DataAccessException e) {
                    log.error("Database error searching by storage", e);
                }
            }

            // ═══════════════════════════════════════════════════════════
            // FALLBACK STRATEGY: Try spell-corrected query
            // ═══════════════════════════════════════════════════════════
            // If original query failed and we detected a likely typo,
            // try again with the auto-corrected version
            
            if (isLikelyMisspelling && !correctedQuery.equals(kw)) {
                try {
                    List<Phone> correctedResults = rankingService.getRankedPhones(correctedQuery, 100);
                    if (correctedResults != null && !correctedResults.isEmpty()) {
                        log.info("Found {} phones with corrected query '{}' (auto-corrected from '{}')", 
                                correctedResults.size(), correctedQuery, kw);
                        return correctedResults;
                    }
                } catch (Exception e) {
                    log.debug("No ranked results for corrected query: {}", correctedQuery);
                }
            }
            
            // ═══════════════════════════════════════════════════════════
            // FUZZY BRAND MATCHING: Handle brand name typos
            // ═══════════════════════════════════════════════════════════
            // Examples: "oneplsu" → "oneplus", "smasung" → "samsung"
            // Try fuzzy matching against all known brands
            
            try {
                List<Phone> allPhones = phoneRepository.findAll();
                Set<String> uniqueBrands = new HashSet<>();
                for (Phone p : allPhones) {
                    if (p.getBrand() != null) {
                        uniqueBrands.add(p.getBrand().toLowerCase());
                    }
                }
                
                // Check if query is similar to any brand name
                for (String brand : uniqueBrands) {
                    if (isFuzzyBrandMatch(kw, brand) || isFuzzyBrandMatch(correctedQuery, brand)) {
                        List<Phone> brandPhones = phoneRepository.findByBrandIgnoreCase(brand);
                        if (brandPhones != null && !brandPhones.isEmpty()) {
                            log.info("Found {} phones using fuzzy brand match: '{}' → '{}'", 
                                    brandPhones.size(), kw, brand);
                            return brandPhones;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Error in fuzzy brand matching", e);
            }

            // 3) If query matches a brand (exact or partial), prefer brand results
            try {
                List<Phone> byBrandExact = phoneRepository.findByBrandIgnoreCase(correctedQuery);
                if (byBrandExact != null && !byBrandExact.isEmpty()) {
                    log.info("Found {} phones matching brand exactly: {}", byBrandExact.size(), correctedQuery);
                    return byBrandExact;
                }
            } catch (DataAccessException e) {
                log.error("Database error searching by exact brand", e);
            }
            
            try {
                List<Phone> byBrandPartial = phoneRepository.findByBrandIgnoreCaseContaining(correctedQuery);
                if (byBrandPartial != null && !byBrandPartial.isEmpty()) {
                    log.info("Found {} phones matching brand partially: {}", byBrandPartial.size(), correctedQuery);
                    return byBrandPartial;
                }
            } catch (DataAccessException e) {
                log.error("Database error searching by partial brand", e);
            }
            
            // If corrected query didn't work, try original query for brand
            if (!correctedQuery.equals(kw)) {
                try {
                    List<Phone> byBrandExact = phoneRepository.findByBrandIgnoreCase(kw);
                    if (byBrandExact != null && !byBrandExact.isEmpty()) {
                        log.info("Found {} phones matching original brand exactly: {}", byBrandExact.size(), kw);
                        return byBrandExact;
                    }
                    
                    List<Phone> byBrandPartial = phoneRepository.findByBrandIgnoreCaseContaining(kw);
                    if (byBrandPartial != null && !byBrandPartial.isEmpty()) {
                        log.info("Found {} phones matching original brand partially: {}", byBrandPartial.size(), kw);
                        return byBrandPartial;
                    }
                } catch (DataAccessException e) {
                    log.error("Database error searching by original brand", e);
                }
            }

            // 4) Use page ranking for generic keyword search to get best matches first
            List<Phone> res = Collections.emptyList();
            try {
                // Get ranked results using RankingService (exact matches appear first)
                List<Phone> rankedResults = rankingService.getRankedPhones(correctedQuery, 100);
                
                if (rankedResults != null && !rankedResults.isEmpty()) {
                    log.info("Found {} ranked phones for query '{}'", rankedResults.size(), correctedQuery);
                    return rankedResults;
                }
                
                // If ranked search didn't work, try original query
                if (!correctedQuery.equals(kw)) {
                    rankedResults = rankingService.getRankedPhones(kw, 100);
                    if (rankedResults != null && !rankedResults.isEmpty()) {
                        log.info("Found {} ranked phones for original query '{}'", rankedResults.size(), kw);
                        return rankedResults;
                    }
                }
                
                // Fallback to basic keyword search
                res = phoneRepository.searchByKeyword(correctedQuery);
                log.info("Search with corrected query '{}': {} results", correctedQuery, res != null ? res.size() : 0);
                
                if (res == null || res.isEmpty()) {
                    // If corrected query didn't work, try original query
                    if (!correctedQuery.equals(kw)) {
                        res = phoneRepository.searchByKeyword(kw);
                        log.info("Search with original query '{}': {} results", kw, res != null ? res.size() : 0);
                    }
                    
                    // If still empty, try ALL spell suggestions one by one
                    if ((res == null || res.isEmpty())) {
                        try {
                            List<String> suggestions = spellCheckService.suggestions(kw, 5);
                            if (suggestions != null && !suggestions.isEmpty()) {
                                log.info("Trying {} spell suggestions for '{}'", suggestions.size(), kw);
                                for (String suggestion : suggestions) {
                                    List<Phone> suggestionResults = phoneRepository.searchByKeyword(suggestion.toLowerCase());
                                    if (suggestionResults != null && !suggestionResults.isEmpty()) {
                                        log.info("✓ Found {} phones using spell suggestion: '{}' → '{}'", 
                                                suggestionResults.size(), kw, suggestion);
                                        return suggestionResults;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error trying spell suggestions", e);
                        }
                    }
                }
            } catch (DataAccessException e) {
                log.error("Database error during keyword search", e);
                return Collections.emptyList();
            }
            
            if (res != null && !res.isEmpty()) {
                log.info("Found {} phones for keyword: {}", res.size(), keyword);
            } else {
                log.info("No phones found for keyword: {}", keyword);
            }
            
            return res != null ? res : Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Unexpected error during search for keyword: {}", keyword, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Fuzzy Brand Matching - Check if query is similar to brand name
     * Handles typos like "oneplsu" → "oneplus", "smasung" → "samsung"
     * 
     * @param query User's search query
     * @param brand Actual brand name from database
     * @return true if they're similar enough
     */
    private boolean isFuzzyBrandMatch(String query, String brand) {
        if (query == null || brand == null) return false;
        if (query.equals(brand)) return true;
        
        // For very short strings, require exact match
        if (query.length() < 3 || brand.length() < 3) {
            return query.equals(brand);
        }
        
        int distance = editDistance(query.toLowerCase(), brand.toLowerCase());
        int maxLength = Math.max(query.length(), brand.length());
        
        // Allow up to 2 character differences for strings up to 10 chars
        // or up to 20% character differences for longer strings
        // Examples: "oneplsu" vs "oneplus" = 1 difference ✓
        //          "smasung" vs "samsung" = 2 differences ✓
        int threshold = maxLength <= 10 ? 2 : (int) Math.ceil(maxLength * 0.2);
        
        return distance <= threshold;
    }
    
    /**
     * Calculate edit distance (Levenshtein distance) between two strings
     * Measures the minimum number of single-character edits needed
     * 
     * @param a First string
     * @param b Second string
     * @return Number of edits needed to transform a into b
     */
    private int editDistance(String a, String b) {
        int n = a.length();
        int m = b.length();
        
        if (n == 0) return m;
        if (m == 0) return n;
        
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return prev[m];
    }

}
