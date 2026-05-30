package com.example.SmartPhone.repository;

import com.example.SmartPhone.model.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Phone Repository - Database Access Layer for Phone Entities
 * 
 * This interface extends JpaRepository, which gives us powerful database operations
 * without writing any SQL code. Spring Data JPA automatically implements this interface
 * at runtime, providing methods like:
 * - findAll() - Get all phones
 * - findById() - Get specific phone by ID
 * - save() - Insert or update a phone
 * - delete() - Remove a phone
 * 
 * We've also defined custom query methods for specialized searches:
 * - Multi-field keyword search
 * - Brand filtering
 * - Storage and RAM searches
 * - Comparison feature support
 */
public interface PhoneRepository extends JpaRepository<Phone, Long> {

    /**
     * Multi-Field Keyword Search
     * 
     * Searches across multiple phone attributes simultaneously to find matches
     * This is used by the ranking service to find potential matches before scoring them
     * 
     * Searches in:
     * - Brand name (e.g., "Samsung", "Apple")
     * - Model name (e.g., "Galaxy S23", "iPhone 15")
     * - Processor (e.g., "Snapdragon 8 Gen 2")
     * - Special features (e.g., "S Pen", "Face ID")
     * - Camera features (e.g., "Night Mode", "Portrait")
     * - Connectivity (e.g., "5G", "WiFi 6E")
     * 
     * Example: searchByKeyword("samsung") finds all phones with "samsung" in any field
     * 
     * @param kw The keyword to search for (case-insensitive)
     * @return List of phones containing the keyword in any of the searched fields
     */
    @Query("SELECT p FROM Phone p WHERE " +
        "LOWER(p.brand) LIKE CONCAT('%',:kw,'%') OR LOWER(p.model) LIKE CONCAT('%',:kw,'%') OR LOWER(p.processor) LIKE CONCAT('%',:kw,'%') " +
        "OR LOWER(p.specialFeatures) LIKE CONCAT('%',:kw,'%') OR LOWER(p.cameraFeatures) LIKE CONCAT('%',:kw,'%') OR LOWER(p.connectivity) LIKE CONCAT('%',:kw,'%')")
    List<Phone> searchByKeyword(@Param("kw") String kw);

    /**
     * Find Phones by Exact Brand Name
     * 
     * Matches phones where the brand name exactly equals the given value
     * Case-insensitive: "samsung" matches "Samsung", "SAMSUNG", etc.
     * 
     * @param brand The exact brand name to match
     * @return List of phones from that brand
     */
    List<Phone> findByBrandIgnoreCase(String brand);

    /**
     * Find Phones Where Brand Contains Text
     * 
     * More flexible than exact match - finds brands containing the search term
     * Example: "sam" would match "Samsung"
     * 
     * @param brand The text to search for within brand names
     * @return List of phones whose brand contains the search term
     */
    List<Phone> findByBrandIgnoreCaseContaining(String brand);

    /**
     * Find Phones by Storage Capacity
     * 
     * Searches the storage field for matching capacity
     * Handles various formats like "512 GB", "512GB", "512", etc.
     * 
     * Example: "512" finds phones with 512GB storage
     * 
     * @param storage The storage capacity to search for
     * @return List of phones with matching storage
     */
    List<Phone> findByStorageContainingIgnoreCase(String storage);

    /**
     * Find Phones by RAM Size
     * 
     * Searches the RAM field for matching memory size
     * Handles formats like "8GB", "8 GB", "12GB", etc.
     * 
     * @param ram The RAM size to search for
     * @return List of phones with matching RAM
     */
    List<Phone> findByRamContainingIgnoreCase(String ram);

    /**
     * Get All Unique Brand Names
     * 
     * Returns a sorted list of all distinct phone brands in our catalog
     * Used to populate the brand dropdown in the comparison feature
     * 
     * Excludes null values and sorts alphabetically
     * 
     * @return List of unique brand names, sorted A-Z
     */
    @Query("SELECT DISTINCT p.brand FROM Phone p WHERE p.brand IS NOT NULL ORDER BY p.brand")
    List<String> findAllBrands();

    /**
     * Get All Phone Models from a Specific Brand
     * 
     * Returns all phones from the specified brand, sorted by model name
     * Used to populate the model dropdown after user selects a brand
     * 
     * Example: brand="Samsung" returns all Samsung phones alphabetically
     * 
     * @param brand The brand name to filter by
     * @return List of phones from that brand, sorted by model name
     */
    List<Phone> findByBrandOrderByModelAsc(String brand);

}
