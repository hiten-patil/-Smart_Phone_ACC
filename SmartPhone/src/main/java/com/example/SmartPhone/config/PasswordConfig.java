package com.example.SmartPhone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Password Security Configuration
 * 
 * This configuration class sets up secure password encryption for our application.
 * 
 * Why BCrypt?
 * -----------
 * We use BCrypt hashing algorithm because:
 * - It's specifically designed for password hashing (unlike MD5 or SHA)
 * - Automatically includes salt (random data) to prevent rainbow table attacks
 * - Adaptive: Can be made slower over time as computers get faster
 * - Industry standard for secure password storage
 * 
 * How it works:
 * -------------
 * When user registers:
 * - Plain password: "myPassword123"
 * - BCrypt hash: "$2a$10$abcd...xyz" (60 characters, irreversible)
 * 
 * When user logs in:
 * - User enters: "myPassword123"
 * - We hash it with BCrypt
 * - Compare hashed version to stored hash
 * - If match → Authentication successful
 * 
 * Security benefit:
 * -----------------
 * Even if database is compromised, attackers cannot reverse the hash
 * to get original passwords. They'd have to brute-force each password
 * individually, which is computationally infeasible with BCrypt.
 */
@Configuration
public class PasswordConfig {

    /**
     * Create BCrypt Password Encoder Bean
     * 
     * This bean is used throughout the application for:
     * - Hashing passwords during user registration
     * - Verifying passwords during login
     * 
     * @return BCryptPasswordEncoder instance managed by Spring
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
