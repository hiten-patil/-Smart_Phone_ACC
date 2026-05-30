package com.example.SmartPhone.service;

import com.example.SmartPhone.model.Phone;
import com.example.SmartPhone.repository.PhoneRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Spell Checking and Word Completion Service
 * 
 * This service implements:
 * 1. Spell Checking - Uses edit distance algorithm to find spelling mistakes
 * 2. Word Completion - Provides autocomplete suggestions based on prefix matching
 */
@Service
public class SpellCheckService {

    private final PhoneRepository phoneRepository;
    private final Set<String> vocabulary = new HashSet<>();

    public SpellCheckService(PhoneRepository phoneRepository) {
        this.phoneRepository = phoneRepository;
    }

    /**
     * Build vocabulary from all phone data for spell checking and word completion
     */
    @PostConstruct
    public void buildVocabulary() {
        List<Phone> phones = phoneRepository.findAll();
        for (Phone p : phones) {
            addText(p.getBrand());
            addText(p.getModel());
            addText(p.getProcessor());
            addText(p.getCameraFeatures());
            addText(p.getSpecialFeatures());
            addText(p.getConnectivity());
            addText(p.getOs());
        }
    }

    /**
     * Extract words from text and add to vocabulary
     */
    private void addText(String text) {
        if (text == null) return;
        // split on non-word characters
        String[] tokens = text.split("[^A-Za-z0-9]+");
        for (String t : tokens) {
            if (t == null) continue;
            String w = t.trim().toLowerCase();
            if (w.length() > 0) vocabulary.add(w);
        }
    }

    // ============================================
    // WORD COMPLETION FEATURE
    // ============================================
    
    /**
     * Word Completion: Returns list of words that start with given prefix
     * Used for autocomplete functionality
     * 
     * @param prefix The starting characters to match
     * @param limit Maximum number of suggestions
     * @return List of matching words sorted alphabetically
     */
    public List<String> completions(String prefix, int limit) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        String p = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String w : vocabulary) {
            if (w.startsWith(p)) out.add(w);
        }
        out.sort(Comparator.naturalOrder());
        if (out.size() > limit) return out.subList(0, limit);
        return out;
    }

    // ============================================
    // SPELL CHECKING FEATURE
    // ============================================
    
    /**
     * Spell Checking: Suggests correct spellings using Edit Distance Algorithm
     * Returns words from vocabulary that are closest to the misspelled word
     * 
     * @param word The potentially misspelled word
     * @param limit Maximum number of suggestions
     * @return List of suggested correct spellings
     */
    public List<String> suggestions(String word, int limit) {
        if (word == null || word.isEmpty()) return Collections.emptyList();
        String w = word.toLowerCase();
        // compute edit distance to all vocabulary words and return closest
        PriorityQueue<Map.Entry<String,Integer>> pq = new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
        for (String v : vocabulary) {
            int d = editDistance(w, v);
            pq.offer(new AbstractMap.SimpleEntry<>(v, d));
        }
        List<String> out = new ArrayList<>();
        while (!pq.isEmpty() && out.size() < limit) {
            Map.Entry<String,Integer> e = pq.poll();
            out.add(e.getKey());
        }
        return out;
    }

    /**
     * Edit Distance Algorithm (Levenshtein Distance)
     * Calculates minimum number of single-character edits needed to change one word into another
     * 
     * @param a First word
     * @param b Second word
     * @return Edit distance (number of changes needed)
     */
    private int editDistance(String a, String b) {
        int n = a.length(), m = b.length();
        int[] prev = new int[m+1];
        int[] cur = new int[m+1];
        for (int j=0;j<=m;j++) prev[j]=j;
        for (int i=1;i<=n;i++){
            cur[0]=i;
            for (int j=1;j<=m;j++){
                int cost = a.charAt(i-1)==b.charAt(j-1)?0:1;
                cur[j]=Math.min(Math.min(prev[j]+1, cur[j-1]+1), prev[j-1]+cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[m];
    }

    /**
     * Get all vocabulary words (for debugging/testing)
     */
    public List<String> getVocabulary(int limit) {
        List<String> out = new ArrayList<>(vocabulary);
        out.sort(Comparator.naturalOrder());
        if (out.size() > limit) return out.subList(0, limit);
        return out;
    }
}
