package com.example.SmartPhone.model;

import jakarta.persistence.*;

/**
 * Word Index Entity - Inverted Index for Fast Phone Search
 * 
 * This entity represents a single entry in our inverted index data structure.
 * Maps to the 'word_index' table in PostgreSQL.
 * 
 * What is an Inverted Index?
 * --------------------------
 * Instead of asking "What words does Phone #5 contain?", we flip it around:
 * "Which phones contain the word 'samsung'?"
 * 
 * Structure:
 * Each entry maps: ONE WORD → ONE PHONE → FREQUENCY COUNT
 * 
 * Example entries:
 * | word      | phone_id | count |
 * | --------- | -------- | ----- |
 * | samsung   | 1        | 3     | "samsung" appears 3 times in phone #1
 * | samsung   | 5        | 1     | "samsung" appears 1 time in phone #5
 * | camera    | 1        | 5     | "camera" appears 5 times in phone #1
 * | 8gb       | 1        | 2     | "8gb" appears 2 times in phone #1
 * 
 * Why This Matters:
 * -----------------
 * Without index: To find "samsung", scan all 105 phones → SLOW
 * With index: Direct lookup "samsung" → Get [1, 5] immediately → FAST
 * 
 * The count field helps with ranking:
 * - Higher count = Word appears more often in that phone = More relevant
 * - Phone with count=3 is likely more about "samsung" than count=1
 * 
 * Unique Constraint:
 * - Prevents duplicate entries for same word-phone combination
 * - Ensures each (word, phone_id) pair appears only once
 */
@Entity
@Table(name = "word_index", uniqueConstraints = {@UniqueConstraint(columnNames = {"word","phone_id"})})
public class WordIndex {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Unique identifier for this index entry

    private String word;  // The indexed word (e.g., "samsung", "camera", "5g")

    @Column(name = "phone_id")
    private Long phoneId;  // Which phone contains this word (foreign key to Phone table)

    private Integer count;  // How many times this word appears in that phone

    /**
     * Default constructor - Required by JPA/Hibernate
     */
    public WordIndex() {}

    // ═══════════════════════════════════════════════════════════
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    
    public Long getPhoneId() { return phoneId; }
    public void setPhoneId(Long phoneId) { this.phoneId = phoneId; }
    
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
