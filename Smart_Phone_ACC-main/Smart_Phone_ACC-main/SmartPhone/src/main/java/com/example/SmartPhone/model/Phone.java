package com.example.SmartPhone.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Phone Entity - Represents a Smartphone in our Catalog
 * 
 * This is our main data model that stores complete specifications for each phone.
 * It maps directly to the 'data' table in our PostgreSQL database.
 * 
 * Each Phone object contains:
 * - Basic info (brand, model, price)
 * - Display specifications (size, resolution, refresh rate)
 * - Camera details (megapixels, lenses, features)
 * - Battery information (capacity, fast charging)
 * - Hardware specs (processor, RAM, storage)
 * - Build and connectivity features
 * - Operating system and special features
 * 
 * This comprehensive model allows users to make informed purchasing decisions
 * by comparing detailed specifications side-by-side.
 */
@Entity
@Table(name = "data")  // Maps to PostgreSQL table named 'data'
public class Phone {
    
    // ═══════════════════════════════════════════════════════════
    // PRIMARY KEY & BASIC INFORMATION
    // ═══════════════════════════════════════════════════════════
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment ID
    private Long id;  // Unique identifier for each phone in database

    private String brand;  // Manufacturer (e.g., "Samsung", "Apple", "OnePlus")
    private String model;  // Model name (e.g., "Galaxy S23 Ultra", "iPhone 15 Pro")

    @Column(name = "price_cad")
    private Double priceCAD;  // Price in Canadian Dollars (used for trending phones)

    @Column(name = "source_url")
    private String sourceUrl;  // URL where phone details were scraped from

    @Column(name = "image_url")
    private String imageUrl;  // Product image URL (currently not used in UI)

    // ═══════════════════════════════════════════════════════════
    // HARDWARE SPECIFICATIONS
    // ═══════════════════════════════════════════════════════════
    
    private String processor;  // CPU/Chipset (e.g., "Snapdragon 8 Gen 2", "A17 Bionic")
    private String ram;        // Memory size (e.g., "8GB", "12GB", "16GB")
    private String storage;    // Internal storage (e.g., "128GB", "256GB", "512GB", "1TB")

    // ═══════════════════════════════════════════════════════════
    // DISPLAY SPECIFICATIONS
    // ═══════════════════════════════════════════════════════════
    
    @Column(name = "display_size")
    private String displaySize;  // Screen diagonal (e.g., "6.7 inches", "6.1 inches")

    @Column(name = "display_resolution")
    private String displayResolution;  // Screen resolution (e.g., "1440x3200", "1170x2532")

    @Column(name = "refresh_rate")
    private String refreshRate;  // How fast screen updates (e.g., "120Hz", "90Hz")

    // ═══════════════════════════════════════════════════════════
    // CAMERA SPECIFICATIONS
    // ═══════════════════════════════════════════════════════════
    
    @Column(name = "camera_mp")
    private String cameraMP;  // Main camera megapixels (e.g., "50MP", "108MP")

    @Column(name = "camera_lenses")
    private String cameraLenses;  // Number and types of lenses (e.g., "Triple: 50MP + 10MP + 12MP")

    @Column(name = "camera_features")
    private String cameraFeatures;  // Special camera abilities (e.g., "Night Mode", "8K Video")

    // ═══════════════════════════════════════════════════════════
    // BATTERY & CHARGING
    // ═══════════════════════════════════════════════════════════
    
    @Column(name = "battery_capacity")
    private String batteryCapacity;  // Battery size (e.g., "5000mAh", "4323mAh")

    @Column(name = "fast_charging")
    private String fastCharging;  // Fast charging support (e.g., "45W", "30W", "Yes/No")

    @Column(name = "wireless_charging")
    private String wirelessCharging;  // Wireless charging capability (e.g., "15W", "Yes/No")

    // ═══════════════════════════════════════════════════════════
    // CONNECTIVITY & BUILD QUALITY
    // ═══════════════════════════════════════════════════════════
    
    private String audio;  // Audio features (e.g., "Stereo Speakers", "Dolby Atmos")

    private String connectivity;  // Network and connections (e.g., "5G", "WiFi 6E", "Bluetooth 5.3")

    @Column(name = "build_quality")
    private String buildQuality;  // Materials and durability (e.g., "Gorilla Glass Victus", "IP68")

    private String os;  // Operating system (e.g., "Android 14", "iOS 17")

    @Column(name = "special_features")
    private String specialFeatures;  // Unique features (e.g., "S Pen", "Face ID", "Under-display camera")

    // ═══════════════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Default constructor - Required by JPA/Hibernate for entity creation
     */
    public Phone() {}

    // ═══════════════════════════════════════════════════════════
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════
    // These allow controlled access to private fields
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Double getPriceCAD() { return priceCAD; }
    public void setPriceCAD(Double priceCAD) { this.priceCAD = priceCAD; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getProcessor() { return processor; }
    public void setProcessor(String processor) { this.processor = processor; }
    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }
    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }
    public String getDisplaySize() { return displaySize; }
    public void setDisplaySize(String displaySize) { this.displaySize = displaySize; }
    public String getDisplayResolution() { return displayResolution; }
    public void setDisplayResolution(String displayResolution) { this.displayResolution = displayResolution; }
    public String getRefreshRate() { return refreshRate; }
    public void setRefreshRate(String refreshRate) { this.refreshRate = refreshRate; }
    public String getCameraMP() { return cameraMP; }
    public void setCameraMP(String cameraMP) { this.cameraMP = cameraMP; }
    public String getCameraLenses() { return cameraLenses; }
    public void setCameraLenses(String cameraLenses) { this.cameraLenses = cameraLenses; }
    public String getCameraFeatures() { return cameraFeatures; }
    public void setCameraFeatures(String cameraFeatures) { this.cameraFeatures = cameraFeatures; }
    public String getBatteryCapacity() { return batteryCapacity; }
    public void setBatteryCapacity(String batteryCapacity) { this.batteryCapacity = batteryCapacity; }
    public String getFastCharging() { return fastCharging; }
    public void setFastCharging(String fastCharging) { this.fastCharging = fastCharging; }
    public String getWirelessCharging() { return wirelessCharging; }
    public void setWirelessCharging(String wirelessCharging) { this.wirelessCharging = wirelessCharging; }
    public String getAudio() { return audio; }
    public void setAudio(String audio) { this.audio = audio; }
    public String getConnectivity() { return connectivity; }
    public void setConnectivity(String connectivity) { this.connectivity = connectivity; }
    public String getBuildQuality() { return buildQuality; }
    public void setBuildQuality(String buildQuality) { this.buildQuality = buildQuality; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }
    public String getSpecialFeatures() { return specialFeatures; }
    public void setSpecialFeatures(String specialFeatures) { this.specialFeatures = specialFeatures; }
}
