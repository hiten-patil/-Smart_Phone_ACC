package com.example.SmartPhone.repository;

import com.example.SmartPhone.model.WordIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Word Index Repository - Manages the Inverted Index Database
 * 
 * This repository handles our inverted index data structure, which enables
 * lightning-fast search capabilities without scanning all 105+ phones every time.
 * 
 * What is an Inverted Index?
 * ---------------------------
 * Instead of looking through every phone to find "samsung", we maintain a lookup table:
 * 
 * Word      | Phone ID | Count
 * --------- | -------- | -----
 * "samsung" | 1        | 3      (appears 3 times in phone #1)
 * "samsung" | 5        | 1      (appears 1 time in phone #5)
 * "camera"  | 1        | 5      (appears 5 times in phone #1)
 * "camera"  | 3        | 2      (appears 2 times in phone #3)
 * 
 * Benefits:
 * - Instant lookup: "Which phones contain 'samsung'?" → Direct database query
 * - No full table scan: Don't need to read all 105 phones
 * - Frequency data: Know how important a word is to each phone
 * 
 * Used by: RankingService for intelligent page ranking
 */
public interface WordIndexRepository extends JpaRepository<WordIndex, Long> {
    
    /**
     * Find All Index Entries for a Single Word
     * 
     * Returns all phones that contain the specified word, along with frequency counts
     * 
     * Example: findByWord("samsung")
     * Returns all WordIndex entries where word="samsung", showing which phones
     * contain "samsung" and how many times it appears in each
     * 
     * @param word The word to look up in the index
     * @return List of WordIndex entries showing which phones contain this word
     */
    List<WordIndex> findByWord(String word);

    /**
     * Find All Index Entries for Multiple Words
     * 
     * Batch lookup: Given a list of words, find all phones that contain any of them
     * This is more efficient than calling findByWord() multiple times
     * 
     * Example: findByWords(["samsung", "galaxy", "s23"])
     * Returns all phones containing any of these words, used for multi-term queries
     * 
     * @param words List of words to look up simultaneously
     * @return List of WordIndex entries for all matching words
     */
    @Query("SELECT w FROM WordIndex w WHERE w.word IN :words")
    List<WordIndex> findByWords(@Param("words") List<String> words);
}
