package com.example.SmartPhone.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Search Log Entity - Tracks Search Query Analytics
 * 
 * This entity represents a single search term and how often it's been queried.
 * Maps to the 'search_log' table in our PostgreSQL database.
 * 
 * Purpose:
 * - Identify trending searches and popular phones
 * - Understand what users are looking for
 * - Display "Top Searches" on the dashboard
 * - Analytics for improving product catalog
 * 
 * How it works:
 * - When a user searches for "Samsung S23", we check if this query exists
 * - If new: Create entry with count=1, timestamp=now
 * - If exists: Increment count and update timestamp
 * 
 * Example data:
 * | query         | count | lastSearched         |
 * | ------------- | ----- | -------------------- |
 * | samsung s23   | 45    | 2024-01-15 14:30:22 |
 * | iphone 15     | 38    | 2024-01-15 14:25:10 |
 * | oneplus 12    | 12    | 2024-01-15 13:45:33 |
 */
@Entity
@Table(name = "search_log")
public class SearchLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Unique identifier for this search log entry

    private String query;  // The search term users entered (e.g., "Samsung Galaxy S23")
    
    private Long count = 0L;  // How many times this term has been searched (defaults to 0)
    
    private LocalDateTime lastSearched;  // When this term was most recently searched

    /**
     * Default constructor - Required by JPA/Hibernate
     */
    public SearchLog() {}

    // ═══════════════════════════════════════════════════════════
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }

    public LocalDateTime getLastSearched() { return lastSearched; }
    public void setLastSearched(LocalDateTime lastSearched) { this.lastSearched = lastSearched; }
}
