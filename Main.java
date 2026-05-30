package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class Main {

    private WebDriver KmDriver;
    private WebDriverWait KmWait;
    private List<Map<String, String>> KmAllPhoneData;
    private Set<String> KmAllColumns;

    /**
     * Constructor - sets up browser, waits, and storage
     */
    public Main() {
        // Tell Selenium where ChromeDriver is located (update path for your system)
        System.setProperty("webdriver.chrome.driver", "drivers/chromedriver.exe");

        // Chrome options to prevent detection and optimize scraping
        ChromeOptions KmOptions = new ChromeOptions();
        KmOptions.addArguments("--disable-blink-features=AutomationControlled");
        KmOptions.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        KmOptions.addArguments("--start-maximized");
        KmOptions.addArguments("--disable-dev-shm-usage");
        KmOptions.addArguments("--no-sandbox");
        KmOptions.addArguments("--disable-notifications");

        // Disable notifications pop-ups
        Map<String, Object> KmPrefs = new HashMap<>();
        KmPrefs.put("profile.default_content_setting_values.notifications", 2);
        KmOptions.setExperimentalOption("prefs", KmPrefs);
        KmOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        // Initialize WebDriver and Wait
        KmDriver = new ChromeDriver(KmOptions);
        KmWait = new WebDriverWait(KmDriver, Duration.ofSeconds(15));

        // Initialize data storage
        KmAllPhoneData = new ArrayList<>();
        KmAllColumns = new LinkedHashSet<>();

        // Add default columns for CSV
        KmAllColumns.add("Phone Name");
        KmAllColumns.add("Brand");
        KmAllColumns.add("Price");
        KmAllColumns.add("Rating");
        KmAllColumns.add("Number of Ratings");
        KmAllColumns.add("Image URL");
        KmAllColumns.add("Product URL");
    }

    /**
     * Scrape multiple pages of smartphones from Amazon India
     * @param KmSearchKeyword The search term, e.g., "oneplus phones"
     * @param KmNumPages Number of pages to scrape
     * @param KmProductsPerPage Number of products per page
     */
    public void scrapeSmartphones(String KmSearchKeyword, int KmNumPages, int KmProductsPerPage) {
        try {
            System.out.println("Search Keyword: " + KmSearchKeyword);
            System.out.println("Pages to scrape: " + KmNumPages);
            System.out.println("Products per page: " + KmProductsPerPage);

            // Loop through each page
            for (int KmPageNum = 1; KmPageNum <= KmNumPages; KmPageNum++) {
                System.out.println(" CRAWLING PAGE " + KmPageNum + " of " + KmNumPages);

                // Build URL for this page
                String KmSearchUrl = buildAmazonSearchUrl(KmSearchKeyword, KmPageNum);
                System.out.println("Opening URL: " + KmSearchUrl);

                // Load the page
                KmDriver.get(KmSearchUrl);

                // Wait for products to load
                try {
                    KmWait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div[data-component-type='s-search-result']")));
                    System.out.println("✓ Page loaded successfully");
                } catch (Exception e) {
                    System.err.println("✗ Page loading timeout - trying alternative selector");
                    KmWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".s-result-item")));
                }

                // Scroll the page to load lazy-loaded content
                scrollPage();
                Thread.sleep(2000); // Give time for content to appear

                // Find all product elements
                List<WebElement> KmProductElements = KmDriver.findElements(
                        By.cssSelector("div[data-component-type='s-search-result']"));

                System.out.println("Found " + KmProductElements.size() + " products on this page");

                // Limit scraping to productsPerPage
                int KmProductsToScrape = Math.min(KmProductsPerPage, KmProductElements.size());

                // Scrape each product
                int KmScrapedCount = 0;
                for (int KmIndex = 0; KmIndex < KmProductElements.size() && KmScrapedCount < KmProductsToScrape; KmIndex++) {
                    try {
                        WebElement KmProduct = KmProductElements.get(KmIndex);

                        // Scroll product into view for proper loading
                        ((JavascriptExecutor) KmDriver).executeScript(
                                "arguments[0].scrollIntoView({block: 'center'});", KmProduct);
                        Thread.sleep(500);

                        Map<String, String> KmPhoneData = extractProductData(KmProduct, KmIndex + 1);

                        if (KmPhoneData != null && !KmPhoneData.isEmpty()) {
                            KmAllPhoneData.add(KmPhoneData);
                            KmScrapedCount++;
                            System.out.println("  ✓ Product " + KmScrapedCount + "/" + KmProductsToScrape + " scraped");
                        }

                    } catch (Exception e) {
                        System.err.println("  ✗ Error scraping product: " + e.getMessage());
                        continue;
                    }
                }

                System.out.println("\n✓ Completed page " + KmPageNum + " - Scraped " + KmScrapedCount + " products");

                // Short delay between pages to reduce bot detection
                Thread.sleep(2000);
            }

        } catch (Exception e) {
            System.err.println("Fatal error during scraping: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract product data from a single product element
     */
    private Map<String, String> extractProductData(WebElement KmProduct, int KmProductNum) {
        Map<String, String> KmPhoneData = new HashMap<>();

        try {
            // Product Name
            String KmProductName = "N/A";
            try {
                WebElement KmNameElement = KmProduct.findElement(By.cssSelector("h2 a span, h2 span"));
                KmProductName = KmNameElement.getText().trim();
                KmPhoneData.put("Phone Name", KmProductName);
            } catch (NoSuchElementException e) {
                return null; // Skip products without name
            }

            System.out.println("    Scraping: " + KmProductName);

            // Brand extracted from product name
            String KmBrand = extractBrand(KmProductName);
            KmPhoneData.put("Brand", KmBrand);

            // Price
            try {
                WebElement KmPriceElement = KmProduct.findElement(By.cssSelector(".a-price-whole"));
                String KmPrice = KmPriceElement.getText().trim().replace(",", "");
                KmPhoneData.put("Price", "₹" + KmPrice);
            } catch (NoSuchElementException e) {
                KmPhoneData.put("Price", "N/A");
            }

            // Rating
            try {
                WebElement KmRatingElement = KmProduct.findElement(By.cssSelector("span[aria-label*='out of']"));
                String KmRating = KmRatingElement.getAttribute("aria-label").split(" ")[0];
                KmPhoneData.put("Rating", KmRating);
            } catch (NoSuchElementException e) {
                KmPhoneData.put("Rating", "N/A");
            }

            // Number of Ratings
            try {
                WebElement KmRatingsCountElement = KmProduct.findElement(By.cssSelector("span[aria-label*='out of'] + span"));
                String KmRatingsCount = KmRatingsCountElement.getText().trim().replace(",", "");
                KmPhoneData.put("Number of Ratings", KmRatingsCount);
            } catch (NoSuchElementException e) {
                KmPhoneData.put("Number of Ratings", "N/A");
            }

            // Image URL
            try {
                WebElement KmImageElement = KmProduct.findElement(By.cssSelector("img.s-image"));
                String KmImageUrl = KmImageElement.getAttribute("src");
                KmPhoneData.put("Image URL", KmImageUrl);
            } catch (NoSuchElementException e) {
                KmPhoneData.put("Image URL", "N/A");
            }

            // Product URL
            try {
                WebElement KmLinkElement = KmProduct.findElement(By.cssSelector("h2 a"));
                String KmProductUrl = KmLinkElement.getAttribute("href");
                if (KmProductUrl.contains("?")) {
                    KmProductUrl = KmProductUrl.split("\\?")[0];
                }
                KmPhoneData.put("Product URL", "https://www.amazon.in" + KmProductUrl);
            } catch (NoSuchElementException e) {
                KmPhoneData.put("Product URL", "N/A");
            }

            // Feature specifications (up to 5)
            try {
                List<WebElement> KmFeatures = KmProduct.findElements(By.cssSelector(".a-size-base.a-color-base"));
                int KmFeatureNum = 1;
                for (WebElement KmFeature : KmFeatures) {
                    String KmFeatureText = KmFeature.getText().trim();
                    if (!KmFeatureText.isEmpty() && KmFeatureText.length() > 10) {
                        String KmColumnName = "Specification " + KmFeatureNum;
                        KmPhoneData.put(KmColumnName, KmFeatureText);
                        KmAllColumns.add(KmColumnName);
                        KmFeatureNum++;
                        if (KmFeatureNum > 5) break;
                    }
                }
            } catch (Exception e) {
                // Skip if no features
            }

            return KmPhoneData;

        } catch (Exception e) {
            System.err.println("    Error extracting product data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract brand from product name
     */
    private String extractBrand(String KmProductName) {
        String[] KmBrands = {"Samsung", "Xiaomi", "Redmi", "Apple", "iPhone",
                "OnePlus", "Oppo", "Vivo", "Realme", "Motorola",
                "Nokia", "Google", "Pixel", "Poco", "Mi"};

        String KmLowerName = KmProductName.toLowerCase();
        for (String KmBrand : KmBrands) {
            if (KmLowerName.contains(KmBrand.toLowerCase())) {
                return KmBrand;
            }
        }
        return "Other";
    }

    /**
     * Build Amazon search URL
     */
    private String buildAmazonSearchUrl(String KmKeyword, int KmPage) {
        String KmBaseUrl = "https://www.amazon.in/s?k=";
        String KmEncodedKeyword = KmKeyword.replace(" ", "+");
        return KmBaseUrl + KmEncodedKeyword + "&page=" + KmPage;
    }

    /**
     * Scroll page to ensure lazy-loaded content is visible
     */
    private void scrollPage() {
        try {
            JavascriptExecutor KmJs = (JavascriptExecutor) KmDriver;

            for (int i = 0; i < 3; i++) {
                KmJs.executeScript("window.scrollBy(0, 800);");
                Thread.sleep(500);
            }

            KmJs.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(500);

            KmJs.executeScript("window.scrollTo(0, 0);");
            Thread.sleep(500);

        } catch (Exception e) {
            System.err.println("Error scrolling page: " + e.getMessage());
        }
    }

    /**
     * Save scraped data to CSV
     */
    public void saveToCSV(String KmFilename) {
        try (FileWriter KmWriter = new FileWriter(KmFilename)) {
            System.out.println("Saving Data To CSV File");
            System.out.println("Total products scraped: " + KmAllPhoneData.size());
            System.out.println("Total columns: " + KmAllColumns.size());

            List<String> KmColumnList = new ArrayList<>(KmAllColumns);
            KmWriter.append(String.join(",", KmColumnList.stream()
                    .map(this::escapeCSV)
                    .toArray(String[]::new)));
            KmWriter.append("\n");

            for (Map<String, String> KmPhoneData : KmAllPhoneData) {
                List<String> KmRowData = new ArrayList<>();
                for (String KmColumn : KmColumnList) {
                    String KmValue = KmPhoneData.getOrDefault(KmColumn, "N/A");
                    KmRowData.add(escapeCSV(KmValue));
                }
                KmWriter.append(String.join(",", KmRowData));
                KmWriter.append("\n");
            }

            System.out.println("✓ Data saved successfully!");

        } catch (IOException e) {
            System.err.println("Error saving CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Escape special CSV characters
     */
    private String escapeCSV(String KmValue) {
        if (KmValue == null) return "N/A";
        if (KmValue.contains(",") || KmValue.contains("\"") || KmValue.contains("\n")) {
            return "\"" + KmValue.replace("\"", "\"\"") + "\"";
        }
        return KmValue;
    }

    /**
     * Close browser
     */
    public void close() {
        if (KmDriver != null) {
            KmDriver.quit();
            System.out.println("✓ Browser closed successfully.");
        }
    }

    /**
     * Display summary of scraping results
     */
    public void displaySummary() {
        System.out.println("Scraping Report");
        System.out.println("Total products scraped: " + KmAllPhoneData.size());
        System.out.println("Total data columns: " + KmAllColumns.size());

        // Count products by brand
        Map<String, Integer> KmBrandCount = new HashMap<>();
        for (Map<String, String> KmData : KmAllPhoneData) {
            String KmBrand = KmData.getOrDefault("Brand", "Unknown");
            KmBrandCount.put(KmBrand, KmBrandCount.getOrDefault(KmBrand, 0) + 1);
        }

        System.out.println("\nColumns collected:");
        int KmCount = 0;
        for (String KmColumn : KmAllColumns) {
            System.out.print("  " + KmColumn);
            KmCount++;
            if (KmCount < KmAllColumns.size()) System.out.print(" | ");
            if (KmCount % 3 == 0) System.out.println();
        }
    }

    /**
     * Main method - Entry point
     */
    public static void main(String[] args) {
        Main KmScraper = new Main();

        try {
            System.out.println("\n");
            System.out.println("                     WEB SCRAPER - Amazon India Edition");
            System.out.println("═══════════════════════════════════════════════════════════════════════════════════════\n");

            // ---------------- CONFIGURATION ----------------
            String KmSearchKeyword = "Oneplus phones";
            int KmNumPages = 2;          // Pages to scrape
            int KmProductsPerPage = 10;   // Products per page

            // Start scraping
            KmScraper.scrapeSmartphones(KmSearchKeyword, KmNumPages, KmProductsPerPage);

            // Show summary
            KmScraper.displaySummary();

            // Save CSV
            String KmCsvFilename = KmSearchKeyword.replace(" ", "_") + ".csv";
            KmScraper.saveToCSV(KmCsvFilename);

            System.out.println("Check your project folder for: " + KmCsvFilename);
            System.out.println();

        } catch (Exception e) {
            System.err.println("\n✗ Fatal error occurred:");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            KmScraper.close();
        }
    }
}
