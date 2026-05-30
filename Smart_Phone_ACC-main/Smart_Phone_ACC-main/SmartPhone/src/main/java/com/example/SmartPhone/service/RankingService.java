package com.example.SmartPhone.service;

import com.example.SmartPhone.model.Phone;
import com.example.SmartPhone.model.WordIndex;
import com.example.SmartPhone.repository.PhoneRepository;
import com.example.SmartPhone.repository.WordIndexRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Page Ranking and Data Validation Service
 * 
 * This service implements:
 * 1. Page Ranking - Ranks search results by relevance/importance
 * 2. Data Validation Using Regex - Validates input patterns and formats
 */
@Service
public class RankingService {

    private final PhoneRepository phoneRepository;
    private final WordIndexRepository wordIndexRepository;

    public RankingService(PhoneRepository phoneRepository, WordIndexRepository wordIndexRepository) {
        this.phoneRepository = phoneRepository;
        this.wordIndexRepository = wordIndexRepository;
    }

    // ============================================
    // PAGE RANKING FEATURE
    // ============================================
    
    /**
     * Page Ranking: Rank phones by relevance to search query
     * Intelligently handles both exact matches and misspellings
     * 
     * Ranking Algorithm:
     * - Exact model match gets HIGHEST priority (10000 points)
     *   Example: "Samsung S23" → "Samsung Galaxy S23" gets top score
     * - Fuzzy model match with misspellings (8000 points)
     *   Example: "Smasung S23" → "Samsung Galaxy S23" still scores very high
     * - Partial model match gets high priority (5000 points)
     * - Brand match gets medium priority (1000 points)
     * - Count how many times search terms appear in each phone
     * - Sort results by score (descending)
     * 
     * @param query Search query (can contain misspellings)
     * @return List of (Phone ID, Score) pairs sorted by score
     */
    public List<Map.Entry<Long, Integer>> rankPhonesByQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        // Tokenize query into words using regex
        String[] tokens = lowerQuery.split("[^A-Za-z0-9]+");
        List<String> words = new ArrayList<>();
        for (String t : tokens) {
            if (t != null && t.trim().length() > 0) {
                words.add(t.trim());
            }
        }
        
        if (words.isEmpty()) {
            return Collections.emptyList();
        }

        // Get index entries for all search words
        List<WordIndex> rows = wordIndexRepository.findByWords(words);
        
        // Calculate relevance score for each phone
        Map<Long, Integer> score = new HashMap<>();
        for (WordIndex w : rows) {
            Long phoneId = w.getPhoneId();
            Integer count = w.getCount() == null ? 0 : w.getCount();
            
            // Accumulate scores (total word occurrences)
            score.put(phoneId, score.getOrDefault(phoneId, 0) + count);
        }
        
        // Boost scores for exact and fuzzy matches in model and brand
        List<com.example.SmartPhone.model.Phone> allPhones = phoneRepository.findAll();
        for (com.example.SmartPhone.model.Phone phone : allPhones) {
            Long phoneId = phone.getId();
            int currentScore = score.getOrDefault(phoneId, 0);
            
            String model = phone.getModel() != null ? phone.getModel().toLowerCase() : "";
            String brand = phone.getBrand() != null ? phone.getBrand().toLowerCase() : "";
            String fullText = (brand + " " + model).toLowerCase();
            
            // HIGHEST PRIORITY: Exact match in model or full text
            // "samsung s23" matches "Samsung Galaxy S23"
            if (model.contains(lowerQuery) || fullText.contains(lowerQuery)) {
                currentScore += 10000;
                score.put(phoneId, currentScore);
            }
            // Very High Priority: Fuzzy brand match - handles typos like "oneplsu" → "oneplus"
            else if (isFuzzyMatch(lowerQuery, brand)) {
                currentScore += 9500;
                score.put(phoneId, currentScore);
            }
            // Very High Priority: Fuzzy match - all query words present in model/brand
            // "smasung s23" → checks if "s23" exists even if "smasung" doesn't
            else if (containsMostWords(fullText, words)) {
                // Give high score if at least 50% of words match
                currentScore += 8000;
                score.put(phoneId, currentScore);
            }
            // High priority: Model contains all search words in order
            else if (containsAllWords(model, words)) {
                currentScore += 5000;
                score.put(phoneId, currentScore);
            }
            
            // Medium-High priority: Fuzzy match for any query word against brand
            for (String word : words) {
                if (isFuzzyMatch(word, brand)) {
                    currentScore += 3000;
                    break; // Only add once per phone
                }
            }
            
            // Medium priority: Brand exact match
            if (brand.equalsIgnoreCase(lowerQuery) || words.contains(brand)) {
                currentScore += 1000;
                score.put(phoneId, currentScore);
            }
            
            // Boost score for each matching word in model (partial matches)
            for (String word : words) {
                if (model.contains(word)) {
                    currentScore += 100;
                }
                if (brand.contains(word)) {
                    currentScore += 50;
                }
            }
            
            score.put(phoneId, currentScore);
        }
        
        // Convert to list and sort by score (descending)
        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(score.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        return entries;
    }
    
    /**
     * Helper method to check if text contains all words
     */
    private boolean containsAllWords(String text, List<String> words) {
        if (text == null || words == null) return false;
        for (String word : words) {
            if (!text.contains(word)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Helper method to check if text contains most words (at least 50%)
     * This helps with misspellings where one word might be wrong
     */
    private boolean containsMostWords(String text, List<String> words) {
        if (text == null || words == null || words.isEmpty()) return false;
        
        int matchCount = 0;
        for (String word : words) {
            if (text.contains(word)) {
                matchCount++;
            }
        }
        
        // Return true if at least 50% of words match
        return matchCount >= Math.ceil(words.size() / 2.0);
    }
    
    /**
     * Calculate similarity between two strings using edit distance
     * Returns true if strings are very similar (allowing for typos)
     * 
     * @param a First string
     * @param b Second string
     * @return true if edit distance is small enough
     */
    private boolean isFuzzyMatch(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        
        // For very short strings, require exact match
        if (a.length() < 3 || b.length() < 3) {
            return a.equals(b);
        }
        
        int distance = editDistance(a.toLowerCase(), b.toLowerCase());
        int maxLength = Math.max(a.length(), b.length());
        
        // Allow up to 2 character differences for strings up to 10 chars
        // or up to 20% character differences for longer strings
        int threshold = maxLength <= 10 ? 2 : (int) Math.ceil(maxLength * 0.2);
        
        return distance <= threshold;
    }
    
    /**
     * Calculate edit distance (Levenshtein distance) between two strings
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

    /**
     * Get ranked phones with full details
     * 
     * @param query Search query
     * @param limit Maximum results
     * @return List of phones sorted by relevance
     */
    public List<Phone> getRankedPhones(String query, int limit) {
        List<Map.Entry<Long, Integer>> rankings = rankPhonesByQuery(query);
        List<Phone> result = new ArrayList<>();
        
        for (Map.Entry<Long, Integer> entry : rankings) {
            if (result.size() >= limit) break;
            
            Long phoneId = entry.getKey();
            if (phoneId != null) {
                phoneRepository.findById(phoneId).ifPresent(result::add);
            }
        }
        
        return result;
    }

    // ============================================
    // DATA VALIDATION USING REGULAR EXPRESSIONS
    // ============================================
    
    /**
     * Validate if query contains digits using regex
     * Used to detect storage/RAM/spec searches
     * 
     * @param query Input query
     * @return true if contains numbers
     */
    public boolean containsDigits(String query) {
        if (query == null) return false;
        // Regex pattern: .*\d+.* means "anything, then digits, then anything"
        return query.matches(".*\\d+.*");
    }

    /**
     * Extract only numeric digits from text using regex
     * Example: "512 GB" → "512"
     * 
     * @param text Input text
     * @return String with only digits
     */
    public String extractDigits(String text) {
        if (text == null) return "";
        // Regex replace: [^0-9] means "anything that's NOT 0-9"
        return text.replaceAll("[^0-9]", "");
    }

    /**
     * Validate if query is a storage search (contains "gb", "tb", digits)
     * 
     * @param query Search query
     * @return true if it's a storage-related search
     */
    public boolean isStorageQuery(String query) {
        if (query == null) return false;
        String lower = query.toLowerCase();
        return containsDigits(query) || lower.contains("gb") || lower.contains("tb");
    }

    /**
     * Validate email format using regex
     * 
     * @param email Email address to validate
     * @return true if valid email format
     */
    public boolean isValidEmail(String email) {
        if (email == null) return false;
        // Email regex pattern
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }

    /**
     * Validate phone number format using regex
     * 
     * @param phone Phone number to validate
     * @return true if valid format
     */
    public boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        // Phone number pattern: 10-15 digits, optional +
        return phone.matches("^[+]?[0-9]{10,15}$");
    }

    /**
     * Validate URL format using regex
     * 
     * @param url URL to validate
     * @return true if valid URL format
     */
    public boolean isValidUrl(String url) {
        if (url == null) return false;
        // URL pattern
        String urlPattern = "^https?://[a-zA-Z0-9.-]+(?:/[^\\s]*)?$";
        return url.matches(urlPattern);
    }

    /**
     * Validate price format (numbers with optional decimal)
     * 
     * @param price Price string to validate
     * @return true if valid price format
     */
    public boolean isValidPrice(String price) {
        if (price == null) return false;
        // Price pattern: digits with optional decimal point
        return price.matches("^\\d+(?:\\.\\d{1,2})?$");
    }

    /**
     * Extract price from text using regex
     * Example: "$999.99" → "999.99"
     * 
     * @param text Input text
     * @return Extracted price or null
     */
    public String extractPrice(String text) {
        if (text == null) return null;
        // Remove currency symbols and extract number
        return text.replaceAll("[^0-9.]", "");
    }

    /**
     * Validate if text contains only alphanumeric characters
     * 
     * @param text Text to validate
     * @return true if only letters and numbers
     */
    public boolean isAlphanumeric(String text) {
        if (text == null) return false;
        return text.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * Clean search query by removing special characters
     * Keeps only letters, numbers, and spaces
     * 
     * @param query Search query
     * @return Cleaned query
     */
    public String sanitizeQuery(String query) {
        if (query == null) return "";
        // Remove everything except letters, numbers, spaces
        return query.replaceAll("[^A-Za-z0-9\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
    }

    /**
     * Validate storage format (e.g., "512GB", "1TB")
     * 
     * @param storage Storage string
     * @return true if valid storage format
     */
    public boolean isValidStorageFormat(String storage) {
        if (storage == null) return false;
        // Pattern: number followed by GB or TB
        return storage.matches("^\\d+\\s*(?:GB|TB)$");
    }

    /**
     * Validate RAM format (e.g., "8GB", "16GB RAM")
     * 
     * @param ram RAM string
     * @return true if valid RAM format
     */
    public boolean isValidRamFormat(String ram) {
        if (ram == null) return false;
        // Pattern: number followed by GB (optional RAM)
        return ram.matches("^\\d+\\s*GB(?:\\s*RAM)?$");
    }
}
