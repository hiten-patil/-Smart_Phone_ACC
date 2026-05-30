package com.example.SmartPhone.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pattern Finding Using Regular Expressions Service
 * 
 * This service implements:
 * 1. Finding Patterns - Uses regex to extract and process text patterns
 * 2. Text Tokenization - Splits text using regex patterns
 * 3. Pattern Matching - Validates and extracts data using regex
 */
@Service
public class PatternService {

    // ============================================
    // PATTERN FINDING USING REGEX
    // ============================================
    
    /**
     * Extract all words (alphanumeric sequences) from text using regex
     * Pattern: [^A-Za-z0-9]+ splits on non-alphanumeric characters
     * 
     * @param text Input text to tokenize
     * @return List of extracted words
     */
    public List<String> tokenizeText(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        
        // Regex pattern: split on anything that's NOT a letter or number
        String[] tokens = text.split("[^A-Za-z0-9]+");
        
        List<String> words = new ArrayList<>();
        for (String token : tokens) {
            if (token != null && !token.trim().isEmpty()) {
                words.add(token.trim().toLowerCase());
            }
        }
        return words;
    }

    /**
     * Extract all numbers from text using regex pattern
     * 
     * @param text Input text
     * @return List of numbers found
     */
    public List<String> extractNumbers(String text) {
        if (text == null) return Collections.emptyList();
        
        List<String> numbers = new ArrayList<>();
        // Pattern to find sequences of digits
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        return numbers;
    }

    /**
     * Extract email addresses from text using regex
     * 
     * @param text Input text
     * @return List of email addresses found
     */
    public List<String> extractEmails(String text) {
        if (text == null) return Collections.emptyList();
        
        List<String> emails = new ArrayList<>();
        // Email pattern
        Pattern pattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            emails.add(matcher.group());
        }
        return emails;
    }

    /**
     * Extract URLs from text using regex
     * 
     * @param text Input text
     * @return List of URLs found
     */
    public List<String> extractUrls(String text) {
        if (text == null) return Collections.emptyList();
        
        List<String> urls = new ArrayList<>();
        // URL pattern
        Pattern pattern = Pattern.compile("https?://[a-zA-Z0-9.-]+(?:/[^\\s]*)?");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }

    /**
     * Check if text contains digits using regex
     * 
     * @param text Input text
     * @return true if text contains any digit
     */
    public boolean containsDigits(String text) {
        if (text == null) return false;
        return text.matches(".*\\d+.*");
    }

    /**
     * Extract only digits from text using regex replacement
     * 
     * @param text Input text
     * @return String containing only digits
     */
    public String extractOnlyDigits(String text) {
        if (text == null) return "";
        return text.replaceAll("[^0-9]", "");
    }

    /**
     * Extract storage capacity patterns (e.g., "512GB", "256 GB", "1TB")
     * 
     * @param text Input text
     * @return List of storage patterns found
     */
    public List<String> extractStoragePatterns(String text) {
        if (text == null) return Collections.emptyList();
        
        List<String> storage = new ArrayList<>();
        // Pattern for storage: number followed by GB/TB (with optional space)
        Pattern pattern = Pattern.compile("\\d+\\s*(?:GB|TB|gb|tb)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            storage.add(matcher.group().trim());
        }
        return storage;
    }

    /**
     * Extract RAM patterns (e.g., "8GB RAM", "16 GB")
     * 
     * @param text Input text
     * @return List of RAM patterns found
     */
    public List<String> extractRamPatterns(String text) {
        if (text == null) return Collections.emptyList();
        
        List<String> ram = new ArrayList<>();
        // Pattern for RAM
        Pattern pattern = Pattern.compile("\\d+\\s*GB(?:\\s*RAM)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            ram.add(matcher.group().trim());
        }
        return ram;
    }

    /**
     * Extract camera megapixel patterns (e.g., "108MP", "50 MP")
     * 
     * @param text Input text
     * @return List of camera MP patterns found
     */
    public List<String> extractCameraPatterns(String text) {
        if (text == null) return Collections.emptyList();
        
        List<String> camera = new ArrayList<>();
        // Pattern for camera megapixels
        Pattern pattern = Pattern.compile("\\d+\\s*MP", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            camera.add(matcher.group().trim());
        }
        return camera;
    }

    /**
     * Clean and normalize text by removing special characters
     * Keeps only letters, numbers, and spaces
     * 
     * @param text Input text
     * @return Cleaned text
     */
    public String cleanText(String text) {
        if (text == null) return "";
        // Remove everything except letters, numbers, and spaces
        return text.replaceAll("[^A-Za-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Split text into sentences using regex
     * 
     * @param text Input text
     * @return List of sentences
     */
    public List<String> splitIntoSentences(String text) {
        if (text == null) return Collections.emptyList();
        
        // Split on sentence-ending punctuation
        String[] sentences = text.split("[.!?]+");
        List<String> result = new ArrayList<>();
        
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Validate phone number format using regex
     * 
     * @param phoneNumber Phone number to validate
     * @return true if valid format
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        // Pattern for phone numbers (various formats)
        return phoneNumber.matches("^[+]?[0-9]{10,15}$");
    }

    /**
     * Extract hashtags from text
     * 
     * @param text Input text
     * @return List of hashtags
     */
    public List<String> extractHashtags(String text) {
        if (text == null) return Collections.emptyList();
        
        List<String> hashtags = new ArrayList<>();
        Pattern pattern = Pattern.compile("#[a-zA-Z0-9_]+");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            hashtags.add(matcher.group());
        }
        return hashtags;
    }
}
