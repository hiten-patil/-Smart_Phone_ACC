package com.example.SmartPhone.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * User Entity - Represents a Registered User Account
 * 
 * This entity stores information about users who have registered on our platform.
 * Maps to the 'users' table in PostgreSQL database.
 * 
 * Purpose:
 * - User authentication and authorization
 * - Personalized user experience
 * - Account management and profile information
 * 
 * Security Features:
 * - Passwords are hashed using BCrypt (never stored in plain text)
 * - Email and username must be unique (prevents duplicates)
 * - All fields are validated before saving
 * 
 * User Journey:
 * 1. User fills registration form with email, username, password, etc.
 * 2. Password is hashed using BCryptPasswordEncoder
 * 3. User object is saved to database
 * 4. User can log in with username/password
 * 5. Session tracks currently logged-in user
 */
@Entity
@Table(name = "users")
public class User {

    // ═══════════════════════════════════════════════════════════
    // PRIMARY KEY & UNIQUE IDENTIFIERS
    // ═══════════════════════════════════════════════════════════
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Unique database ID for this user

    @Column(nullable = false, unique = true)
    private String email;  // Email address (must be unique, used for notifications)

    @Column(nullable = false, unique = true)
    private String username;  // Login username (must be unique)

    @Column(nullable = false)
    private String password;  // BCrypt hashed password (NOT plain text!)

    // ═══════════════════════════════════════════════════════════
    // PERSONAL INFORMATION
    // ═══════════════════════════════════════════════════════════
    
    @Column(name = "first_name")
    private String firstName;  // User's first name (for personalization)

    @Column(name = "last_name")
    private String lastName;  // User's last name

    @Column(name = "phone_number")
    private String phoneNumber;  // Contact phone number (optional)

    private LocalDate dob;  // Date of birth (for age verification or demographics)

    // ═══════════════════════════════════════════════════════════
    // METADATA
    // ═══════════════════════════════════════════════════════════
    
    private LocalDateTime createdAt = LocalDateTime.now();  // When account was created

    /**
     * Default constructor - Required by JPA/Hibernate
     */
    public User() {}

    // ═══════════════════════════════════════════════════════════
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════
    // These provide controlled access to private fields
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
