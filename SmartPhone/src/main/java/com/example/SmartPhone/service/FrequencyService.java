package com.example.SmartPhone.service;

import com.example.SmartPhone.model.SearchLog;
import com.example.SmartPhone.repository.SearchLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Frequency Count and Search Frequency Service
 * 
 * This service implements:
 * 1. Frequency Count - Counts word occurrences in URLs/webpages
 * 2. Search Frequency - Tracks and displays most searched terms
 */
@Service
public class FrequencyService {

    private final SearchLogRepository repo;

    public FrequencyService(SearchLogRepository repo) {
        this.repo = repo;
    }

    // ============================================
    // FREQUENCY COUNT FEATURE
    // ============================================
    
    /**
     * Frequency Count: Counts how many times a word appears on a webpage
     * Fetches the webpage and scans its text content
     * 
     * @param url The webpage URL to analyze
     * @param word The word to count
     * @return Number of occurrences (or -1 if error)
     */
    public int countWordInUrl(String url, String word) {
        if (url == null || word == null) return 0;
        try {
            // Fetch webpage using Jsoup
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();
            
            // Get all text from page
            String text = doc.text().toLowerCase();
            String w = word.toLowerCase();
            
            // Count occurrences
            int count = 0;
            int idx = 0;
            while ((idx = text.indexOf(w, idx)) != -1) {
                count++;
                idx += w.length();
            }
            return count;
        } catch (Exception e) {
            return -1; // indicate error
        }
    }

    // ============================================
    // SEARCH FREQUENCY FEATURE
    // ============================================
    
    /**
     * Search Frequency: Log every search query made by users
     * Stores query, count, and timestamp in database
     * 
     * @param query The search term entered by user
     */
    public void logSearch(String query) {
        if (query == null) return;
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) return;
        
        // Check if this query was searched before
        Optional<SearchLog> ex = repo.findByQuery(q);
        if (ex.isPresent()) {
            // Increment count for existing search
            SearchLog s = ex.get();
            s.setCount(s.getCount() + 1);
            s.setLastSearched(LocalDateTime.now());
            repo.save(s);
        } else {
            // Create new search log entry
            SearchLog s = new SearchLog();
            s.setQuery(q);
            s.setCount(1L);
            s.setLastSearched(LocalDateTime.now());
            repo.save(s);
        }
    }

    /**
     * Get most frequently searched terms
     * Returns trending/popular searches sorted by count
     * 
     * @param limit Maximum number of results
     * @return List of most searched terms
     */
    public List<SearchLog> topSearches(int limit) {
        List<SearchLog> all = repo.findAll();
        // Sort by count descending (most searched first)
        all.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        if (all.size() > limit) return all.subList(0, limit);
        return all;
    }

    /**
     * Get search history sorted by most recent
     * 
     * @param limit Maximum number of results
     * @return List of recent searches
     */
    public List<SearchLog> recentSearches(int limit) {
        List<SearchLog> all = repo.findAll();
        // Sort by timestamp descending (most recent first)
        all.sort((a, b) -> b.getLastSearched().compareTo(a.getLastSearched()));
        if (all.size() > limit) return all.subList(0, limit);
        return all;
    }
}
