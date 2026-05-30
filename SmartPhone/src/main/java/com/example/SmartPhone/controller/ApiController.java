package com.example.SmartPhone.controller;

import com.example.SmartPhone.model.Phone;
import com.example.SmartPhone.repository.PhoneRepository;
import com.example.SmartPhone.service.SpellCheckService;
import com.example.SmartPhone.service.FrequencyService;
import com.example.SmartPhone.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Phone Search Application
 * 
 * This controller provides JSON-based API endpoints for the frontend JavaScript
 * to interact with our backend services. It handles:
 * 
 * 1. Autocomplete suggestions as users type
 * 2. Intelligent search with ranking algorithms
 * 3. Brand and phone model listings for dropdowns
 * 4. Phone detail retrieval for comparison feature
 * 5. Word frequency analysis for any webpage
 * 
 * All responses are in JSON format to make frontend integration seamless.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    
    // Inject our specialized services that handle different features
    private final RankingService rankingService;        // Intelligent page ranking algorithm
    private final PhoneRepository phoneRepository;      // Direct database access
    private final SpellCheckService spellCheckService;  // Autocomplete and spell suggestions
    private final FrequencyService frequencyService;    // Word frequency counting on webpages

    /**
     * Constructor: Spring automatically injects all required services
     */
    public ApiController(RankingService rankingService, PhoneRepository phoneRepository, 
                        SpellCheckService spellCheckService, FrequencyService frequencyService) {
        this.rankingService = rankingService;
        this.phoneRepository = phoneRepository;
        this.spellCheckService = spellCheckService;
        this.frequencyService = frequencyService;
    }

    /**
     * Autocomplete Suggestions API
     * 
     * As users type in the search box, this endpoint provides instant suggestions
     * Example: User types "sam" → Returns ["samsung", "samsung galaxy", "samsung note"]
     * 
     * Feature: Word Completion (SpellCheckService)
     * 
     * @param q The partial search query typed by user
     * @return List of up to 10 matching words/phrases to help complete their search
     */
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(@RequestParam("q") String q) {
        List<String> c = spellCheckService.completions(q, 10);
        return ResponseEntity.ok(c);
    }

    /**
     * Intelligent Search API with Ranking
     * 
     * This is our core search functionality that uses advanced ranking algorithms
     * to show the most relevant results first. It considers:
     * - Exact matches (highest priority)
     * - Fuzzy matches (handles typos)
     * - Partial matches (brand or model contains the search term)
     * - Brand-level matches (lowest priority)
     * 
     * Feature: Page Ranking (RankingService)
     * 
     * Example: Searching "Samsung S23" will show "Samsung Galaxy S23" at the top,
     * even if user misspelled it as "Smasung S23"
     * 
     * @param q User's search query
     * @return List of phones ranked by relevance, with scores indicating match quality
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String,Object>>> search(@RequestParam("q") String q) {
        List<Map<String,Object>> out = new ArrayList<>();
        
        // Try using our intelligent ranking system first
        try {
            List<java.util.Map.Entry<Long,Integer>> ranks = rankingService.rankPhonesByQuery(q);
            
            // Convert each ranked phone ID into a full phone object with details
            for (java.util.Map.Entry<Long,Integer> e : ranks) {
                Long phoneId = e.getKey();
                if (phoneId != null) {
                    phoneRepository.findById(phoneId).ifPresent(p -> {
                        Map<String,Object> m = new HashMap<>();
                        m.put("id", p.getId());
                        m.put("brand", p.getBrand());
                        m.put("model", p.getModel());
                        m.put("priceCAD", p.getPriceCAD());
                        m.put("score", e.getValue());  // Ranking score for debugging
                        m.put("sourceUrl", p.getSourceUrl());
                        out.add(m);
                    });
                }
            }
        } catch (Exception ex) {
            // FALLBACK STRATEGY: If ranking fails, do a simple text-based search
            // This ensures users always get some results even if the ranking system has issues
            log.warn("Ranking search failed, using fallback simple search", ex);
            
            phoneRepository.findAll().stream().filter(p -> {
                // Search across multiple fields: brand, model, processor, features
                String searchableText = (p.getBrand()+" "+p.getModel()+" "+p.getProcessor()+" "+p.getSpecialFeatures()).toLowerCase();
                return searchableText.contains(q.toLowerCase());
            }).forEach(p -> {
                Map<String,Object> m = new HashMap<>();
                m.put("id", p.getId());
                m.put("brand", p.getBrand());
                m.put("model", p.getModel());
                m.put("priceCAD", p.getPriceCAD());
                m.put("score", 0);  // No scoring in fallback mode
                m.put("sourceUrl", p.getSourceUrl());
                out.add(m);
            });
        }
        return ResponseEntity.ok(out);
    }

    /**
     * Get All Phone Brands API
     * 
     * Returns a list of unique phone brands available in our database
     * Used to populate the brand dropdown in the comparison feature
     * 
     * Example response: ["Samsung", "Apple", "OnePlus", "Google", "Xiaomi"]
     * 
     * @return List of distinct brand names sorted alphabetically
     */
    @GetMapping("/brands")
    public ResponseEntity<List<String>> getBrands() {
        List<String> brands = phoneRepository.findAllBrands();
        return ResponseEntity.ok(brands);
    }

    /**
     * Get Phone Models by Brand API
     * 
     * When user selects a brand (e.g., "Samsung"), this returns all models
     * from that brand to populate the model dropdown in the comparison feature
     * 
     * Example: brand="Samsung" → Returns all Samsung phone models
     * 
     * @param brand The brand name to filter by
     * @return List of phones from that brand, sorted by model name
     */
    @GetMapping("/phones")
    public ResponseEntity<List<Map<String,Object>>> getPhonesByBrand(@RequestParam("brand") String brand) {
        List<Phone> phones = phoneRepository.findByBrandOrderByModelAsc(brand);
        List<Map<String,Object>> result = new ArrayList<>();
        
        // Convert each Phone entity to a lightweight JSON object
        for (Phone p : phones) {
            Map<String,Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("brand", p.getBrand());
            m.put("model", p.getModel());
            result.add(m);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get Phone Details by ID API
     * 
     * Retrieves complete specifications for a single phone
     * Used primarily in the comparison feature to show full details
     * 
     * @param id The unique database ID of the phone
     * @return Full phone object with all specifications, or 404 if not found
     */
    @GetMapping("/phone/{id}")
    public ResponseEntity<Phone> getPhoneById(@PathVariable("id") Long id) {
        log.info("Fetching phone details for ID: {}", id);
        
        // Validate that ID is not null
        if (id == null) {
            log.warn("Phone ID is null");
            return ResponseEntity.badRequest().build();
        }
        
        // Try to find the phone in database
        return phoneRepository.findById(id)
                .map(phone -> {
                    log.info("Found phone: {} {}", phone.getBrand(), phone.getModel());
                    return ResponseEntity.ok(phone);
                })
                .orElseGet(() -> {
                    // Phone doesn't exist - return 404 Not Found
                    log.warn("Phone not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Word Frequency Analysis API
     * 
     * Counts how many times a specific word appears on a given webpage
     * This is useful for SEO analysis and content research
     * 
     * Feature: Frequency Count (FrequencyService)
     * 
     * Example: url="https://example.com", word="phone" → Returns count of "phone" on that page
     * 
     * @param url  The webpage URL to analyze
     * @param word The word to count occurrences of
     * @return JSON object with count and success/error information
     */
    @GetMapping("/word-frequency")
    public ResponseEntity<Map<String,Object>> getWordFrequency(
            @RequestParam("url") String url, 
            @RequestParam("word") String word) {
        
        Map<String,Object> result = new HashMap<>();
        int count = frequencyService.countWordInUrl(url, word);
        
        // Check if the fetch failed (indicated by -1)
        if (count == -1) {
            result.put("success", false);
            result.put("error", "Failed to fetch URL or invalid URL");
            result.put("url", url);
            result.put("word", word);
            return ResponseEntity.badRequest().body(result);
        }
        
        // Success - return the frequency count
        result.put("success", true);
        result.put("url", url);
        result.put("word", word);
        result.put("count", count);
        result.put("message", "The word '" + word + "' appears " + count + " times in the URL");
        
        return ResponseEntity.ok(result);
    }
}
