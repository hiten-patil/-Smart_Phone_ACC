package com.example.SmartPhone.repository;

import com.example.SmartPhone.model.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Search Log Repository - Tracks User Search Queries
 * 
 * This repository manages the search_log table, which stores analytics about
 * what users are searching for on our platform.
 * 
 * Purpose:
 * - Track popular search terms to improve our product catalog
 * - Identify trending phones based on search frequency
 * - Help understand user intent and behavior
 * 
 * Each SearchLog entry contains:
 * - query: The search term (e.g., "Samsung S23", "iPhone 15")
 * - count: How many times it's been searched
 * - timestamp: When it was last searched
 * 
 * This data powers the "Top Searches" feature shown on the dashboard.
 */
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
    
    /**
     * Find Search Log Entry by Query Text
     * 
     * Looks up a search query in our logs to see if it's been searched before
     * This allows us to increment the count instead of creating duplicate entries
     * 
     * Example: User searches "Samsung Galaxy"
     * - First time: Creates new SearchLog with count=1
     * - Second time: Finds existing log and updates count=2
     * - Third time: Finds existing log and updates count=3
     * 
     * @param query The search term to look up
     * @return Optional containing the SearchLog if found, empty if first time searching this term
     */
    Optional<SearchLog> findByQuery(String query);
}
