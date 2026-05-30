package com.example.SmartPhone.controller;

import com.example.SmartPhone.model.Phone;
import com.example.SmartPhone.service.CsvService;
import com.example.SmartPhone.service.SpellCheckService;
import com.example.SmartPhone.service.FrequencyService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

/**
 * Main Dashboard Controller
 * 
 * This controller manages the primary user interface where visitors can:
 * - Browse and search through our smartphone catalog
 * - View trending devices based on popularity
 * - Get intelligent search suggestions with spell correction
 * - Navigate through paginated results (9 items per page)
 * 
 * The dashboard integrates multiple advanced features including spell checking,
 * search frequency tracking, and intelligent ranking to provide the best user experience.
 */
@Controller
public class DashboardController {

    // Logger helps us track what's happening in our application for debugging
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    
    // These services handle different aspects of our phone search functionality
    private final CsvService csvService;              // Handles phone data retrieval and search
    private final FrequencyService frequencyService;  // Tracks which terms users search most often
    private final SpellCheckService spellCheckService; // Provides autocomplete and spell suggestions

    /**
     * Constructor: Sets up the dashboard with all required services
     * Spring automatically injects these dependencies when creating the controller
     */
    public DashboardController(CsvService csvService, FrequencyService frequencyService, SpellCheckService spellCheckService) {
        this.csvService = csvService;
        this.frequencyService = frequencyService;
        this.spellCheckService = spellCheckService;
    }

    /**
     * Main Dashboard Page Handler
     * 
     * This method handles requests to both "/" and "/dashboard" URLs
     * It orchestrates the entire dashboard functionality including:
     * 1. User authentication checking
     * 2. Search query processing with spell correction
     * 3. Result pagination (showing 9 phones per page)
     * 4. Trending phones display
     * 5. Search history tracking
     * 
     * @param session HTTP session to check if user is logged in
     * @param model   Model object to pass data to the view
     * @param q       Optional search query parameter from user
     * @param page    Current page number for pagination (defaults to 1)
     * @return Name of the template to render (dashboard.html)
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(HttpSession session, Model model, 
                          @RequestParam(value = "q", required = false) String q,
                          @RequestParam(value = "page", defaultValue = "1") int page) {
        try {
            // First, let's check if someone is logged in and greet them
            Object cu = session.getAttribute("currentUser");
            if (cu != null) {
                model.addAttribute("username", cu.toString());
            }

            // Safety check: Make sure search queries aren't too long (prevents abuse)
            if (q != null && q.trim().length() > 200) {
                log.warn("Search query too long: {} characters", q.length());
                model.addAttribute("error", "Search query is too long. Please use fewer characters.");
                q = null; // Reset to show all phones instead
            }
            
            // Safety check: Page numbers must be positive
            if (page < 1) {
                page = 1;
            }

            // Initialize empty lists - we'll fill these based on whether user is searching
            List<Phone> trending = Collections.emptyList();
            List<Phone> all = Collections.emptyList();
            
            // Try to fetch trending phones (top 12 most popular based on search frequency)
            try {
                trending = csvService.topTrending(12);
            } catch (Exception e) {
                log.error("Error fetching trending phones", e);
                // If trending fails, we continue - it's not critical to the main functionality
            }
            
            // Check if user entered a search query
            if (q != null && !q.trim().isEmpty()) {
                // USER IS SEARCHING - Show search results only, hide trending section
                try {
                    log.info("Searching for: {}", q);
                    
                    // SPELL CORRECTION FEATURE:
                    // Check if the user's search term might have a typo
                    // For example: "appel" → suggests "apple", "smasung" → suggests "samsung"
                    List<String> suggestions = spellCheckService.suggestions(q.trim(), 1);
                    String correctedTerm = null;
                    if (suggestions != null && !suggestions.isEmpty()) {
                        String firstSuggestion = suggestions.get(0);
                        // Only suggest if it's actually different from what user typed
                        if (!firstSuggestion.equalsIgnoreCase(q.trim())) {
                            correctedTerm = firstSuggestion;
                        }
                    }
                    
                    // Execute the search using our intelligent ranking system
                    all = csvService.search(q.trim());
                    model.addAttribute("query", q);
                    model.addAttribute("searching", true);
                    
                    // Add spell correction hints to help user:
                    // Case 1: No results found - show "Did you mean...?"
                    if (correctedTerm != null && (all == null || all.isEmpty())) {
                        model.addAttribute("spellSuggestion", correctedTerm);
                        log.info("Suggesting spell correction: {} → {}", q, correctedTerm);
                    } 
                    // Case 2: Found results with correction - show "Showing results for..."
                    else if (correctedTerm != null && all != null && !all.isEmpty()) {
                        model.addAttribute("usedSpellCorrection", correctedTerm);
                        log.info("Used spell correction: {} → {} (found {} results)", q, correctedTerm, all.size());
                    }
                    
                    // Handle no results scenario
                    if (all == null || all.isEmpty()) {
                        log.info("No results found for query: {}", q);
                        model.addAttribute("noResults", true);
                        all = Collections.emptyList();
                    } else {
                        log.info("Found {} results for query: {}", all.size(), q);
                    }
                } catch (Exception e) {
                    log.error("Error during search for query: {}", q, e);
                    model.addAttribute("error", "Search failed. Showing all phones instead.");
                    // Fallback: Show all phones if search fails
                    all = csvService.readAllPhones();
                    model.addAttribute("searching", false);
                }
            } else {
                // NO SEARCH - Display all phones in catalog with trending section
                try {
                    all = csvService.readAllPhones();
                    model.addAttribute("searching", false);
                } catch (Exception e) {
                    log.error("Error fetching all phones", e);
                    model.addAttribute("error", "Unable to load phones. Please try again later.");
                    all = Collections.emptyList();
                }
            }

            // POPULATE SEARCH HISTORY AND AUTOCOMPLETE SUGGESTIONS
            // These features enhance user experience by showing popular searches and word options
            try {
                // Show top 3 most searched terms to help users discover popular phones
                model.addAttribute("topSearches", frequencyService.topSearches(3));
            } catch (Exception e) {
                log.error("Error fetching top searches", e);
                model.addAttribute("topSearches", Collections.emptyList());
            }
            
            try {
                // Load vocabulary (up to 200 words) for autocomplete suggestions
                // This helps users type faster and find what they're looking for
                model.addAttribute("vocab", spellCheckService.getVocabulary(200));
            } catch (Exception e) {
                log.error("Error fetching vocabulary", e);
                model.addAttribute("vocab", Collections.emptyList());
            }

            // ═══════════════════════════════════════════════════════════
            // PAGINATION SYSTEM - Shows 9 phones per page
            // ═══════════════════════════════════════════════════════════
            // This creates a smooth browsing experience by breaking large
            // result sets into manageable chunks
            
            int itemsPerPage = 9;  // Display exactly 9 phone cards per page
            int totalItems = all.size();  // How many phones do we have in total?
            
            // Calculate how many pages we need to display all phones
            // Example: 25 phones ÷ 9 per page = 2.77 → rounds up to 3 pages
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            
            log.info("Pagination info - Total items: {}, Items per page: {}, Total pages: {}, Current page: {}", 
                    totalItems, itemsPerPage, totalPages, page);
            
            // Boundary check: Don't let users go beyond the last page
            if (page > totalPages && totalPages > 0) {
                page = totalPages;
            }
            
            // Calculate which phones to show on the current page
            // Example: Page 2 with 9 per page → start at index 9 (phones 10-18)
            int startIndex = (page - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
            
            // Extract just the phones for this page from the full list
            List<Phone> paginatedPhones = Collections.emptyList();
            if (!all.isEmpty() && startIndex < totalItems) {
                paginatedPhones = all.subList(startIndex, endIndex);
                log.info("Showing phones {} to {} (page {} of {})", 
                        startIndex + 1, endIndex, page, totalPages);
            }

            // Send all the data to the view template
            model.addAttribute("trending", trending);
            model.addAttribute("phones", paginatedPhones);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);
            
        } catch (Exception e) {
            // Global error handler: If anything unexpected happens, show a friendly message
            log.error("Unexpected error in dashboard", e);
            model.addAttribute("error", "An unexpected error occurred. Please try again.");
            model.addAttribute("phones", Collections.emptyList());
            model.addAttribute("trending", Collections.emptyList());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 0);
        }
        
        return "dashboard";  // Render the dashboard.html template
    }

    /**
     * Phone Comparison Page Handler
     * 
     * Displays a side-by-side comparison interface where users can:
     * - Select two phones to compare
     * - View detailed specifications side-by-side
     * - Make informed purchasing decisions
     * 
     * @param session HTTP session for user authentication
     * @param model   Model to pass user data to view
     * @return Template name for comparison page
     */
    @GetMapping("/comparison")
    public String comparison(HttpSession session, Model model) {
        try {
            // Add username if user is logged in
            Object cu = session.getAttribute("currentUser");
            if (cu != null) {
                model.addAttribute("username", cu.toString());
            }
            return "comparison";
        } catch (Exception e) {
            log.error("Error accessing comparison page", e);
            // If something goes wrong, send them back to the main dashboard
            return "redirect:/dashboard";
        }
    }

    /**
     * Word Frequency Analyzer Page Handler
     * 
     * Provides a tool for analyzing word frequency on web pages
     * Users can enter a URL and a word to see how many times it appears
     * 
     * This is useful for SEO analysis and content research
     * 
     * @param session HTTP session for user authentication
     * @param model   Model to pass user data to view
     * @return Template name for word frequency page
     */
    @GetMapping("/word-frequency")
    public String wordFrequency(HttpSession session, Model model) {
        try {
            // Add username if user is logged in
            Object cu = session.getAttribute("currentUser");
            if (cu != null) {
                model.addAttribute("username", cu.toString());
            }
            return "word-frequency";
        } catch (Exception e) {
            log.error("Error accessing word frequency page", e);
            // Redirect to dashboard if there's an error
            return "redirect:/dashboard";
        }
    }
}
