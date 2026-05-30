package com.example.SmartPhone.service;

import com.example.SmartPhone.model.Phone;
import com.example.SmartPhone.model.WordIndex;
import com.example.SmartPhone.repository.PhoneRepository;
import com.example.SmartPhone.repository.WordIndexRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Inverted Indexing Service
 * 
 * This service implements:
 * 1. Inverted Indexing - Creates index mapping words to document IDs
 * 2. Quick Search - Fast lookup without scanning all documents
 */
@Service
public class IndexingService {

    private final PhoneRepository phoneRepository;
    private final WordIndexRepository wordIndexRepository;

    public IndexingService(PhoneRepository phoneRepository, WordIndexRepository wordIndexRepository) {
        this.phoneRepository = phoneRepository;
        this.wordIndexRepository = wordIndexRepository;
    }

    // ============================================
    // INVERTED INDEXING FEATURE
    // ============================================
    
    /**
     * Build Inverted Index from all phone data
     * Creates mapping: word -> list of phone IDs where word appears
     * Also stores frequency count for each word-phone pair
     * 
     * Index Structure:
     * ----------------
     * Word      | Phone ID | Count
     * "samsung" | 1        | 3
     * "samsung" | 5        | 1
     * "camera"  | 1        | 5
     * "camera"  | 3        | 2
     * 
     * This allows instant lookup: "Which phones contain 'samsung'?"
     * Instead of scanning all 105 phones, directly lookup in index
     */
    @Transactional
    public void rebuildIndex() {
        System.out.println("Starting inverted index rebuild...");
        
        List<Phone> phones = phoneRepository.findAll();
        
        // Clear existing index
        wordIndexRepository.deleteAll();
        System.out.println("Cleared old index");

        int processedPhones = 0;
        for (Phone p : phones) {
            // Count word frequencies in this phone's text
            Map<String, Integer> freq = new HashMap<>();
            
            // Add text from all phone fields
            addTextTokens(freq, p.getBrand());
            addTextTokens(freq, p.getModel());
            addTextTokens(freq, p.getProcessor());
            addTextTokens(freq, p.getCameraFeatures());
            addTextTokens(freq, p.getSpecialFeatures());
            addTextTokens(freq, p.getConnectivity());
            addTextTokens(freq, p.getOs());

            // Create index entry for each word in this phone
            for (Map.Entry<String,Integer> e : freq.entrySet()) {
                WordIndex wi = new WordIndex();
                wi.setWord(e.getKey());
                wi.setPhoneId(p.getId());
                wi.setCount(e.getValue());
                wordIndexRepository.save(wi);
            }
            
            processedPhones++;
        }
        
        System.out.println("Inverted index rebuilt successfully!");
        System.out.println("Processed " + processedPhones + " phones");
    }

    /**
     * Tokenize text and count word frequencies
     * Uses regex pattern to split text into words
     * 
     * @param freq Map to store word frequencies
     * @param text Text to process
     */
    private void addTextTokens(Map<String,Integer> freq, String text) {
        if (text == null) return;
        
        // Split on non-alphanumeric characters using regex
        String[] tokens = text.split("[^A-Za-z0-9]+");
        
        for (String t : tokens) {
            if (t == null) continue;
            String w = t.trim().toLowerCase();
            if (w.length() == 0) continue;
            
            // Increment frequency count
            freq.put(w, freq.getOrDefault(w, 0) + 1);
        }
    }

    /**
     * Search using inverted index
     * Returns phones that contain the search words
     * 
     * @param query Search query (can be multiple words)
     * @return List of phone IDs that match
     */
    public List<Long> searchInIndex(String query) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        
        // Tokenize query into words
        String[] tokens = query.toLowerCase().split("[^A-Za-z0-9]+");
        List<String> words = new ArrayList<>();
        for (String t : tokens) {
            if (t != null && t.trim().length() > 0) {
                words.add(t.trim());
            }
        }
        
        if (words.isEmpty()) return Collections.emptyList();

        // Query index for matching phones
        List<WordIndex> rows = wordIndexRepository.findByWords(words);
        
        // Collect unique phone IDs
        Set<Long> phoneIds = new HashSet<>();
        for (WordIndex w : rows) {
            phoneIds.add(w.getPhoneId());
        }
        
        return new ArrayList<>(phoneIds);
    }

    /**
     * Get all index entries for a specific word
     * Shows which phones contain this word and how many times
     * 
     * @param word Word to lookup
     * @return List of index entries
     */
    public List<WordIndex> getWordEntries(String word) {
        if (word == null) return Collections.emptyList();
        return wordIndexRepository.findByWord(word.toLowerCase());
    }

    /**
     * Get total number of words in index
     * 
     * @return Count of unique words
     */
    public long getIndexSize() {
        return wordIndexRepository.count();
    }

    /**
     * Get most common words in index
     * 
     * @param limit Maximum results
     * @return List of most frequent words
     */
    public List<Map.Entry<String, Long>> getMostCommonWords(int limit) {
        List<WordIndex> allEntries = wordIndexRepository.findAll();
        
        // Aggregate total count per word across all phones
        Map<String, Long> wordCounts = new HashMap<>();
        for (WordIndex entry : allEntries) {
            String word = entry.getWord();
            long count = entry.getCount() == null ? 0 : entry.getCount();
            wordCounts.put(word, wordCounts.getOrDefault(word, 0L) + count);
        }
        
        // Sort by count descending
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(wordCounts.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        if (sorted.size() > limit) {
            return sorted.subList(0, limit);
        }
        return sorted;
    }

    /**
     * Check if index is empty (needs rebuilding)
     * 
     * @return true if index needs to be built
     */
    public boolean isIndexEmpty() {
        return wordIndexRepository.count() == 0;
    }
}
