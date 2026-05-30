package com.example.SmartPhone.controller;

import com.example.SmartPhone.entity.User;
import com.example.SmartPhone.model.RegistrationRequest;
import com.example.SmartPhone.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute RegistrationRequest registrationRequest, 
                           BindingResult bindingResult, Model model) {
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                log.warn("Registration validation failed for username: {}", registrationRequest.getUsername());
                model.addAttribute("error", "Please fix the validation errors");
                return "register";
            }
            
            // Additional input sanitization
            if (registrationRequest.getUsername() == null || registrationRequest.getUsername().trim().isEmpty()) {
                model.addAttribute("error", "Username cannot be empty");
                return "register";
            }
            
            if (registrationRequest.getPassword() == null || registrationRequest.getPassword().trim().isEmpty()) {
                model.addAttribute("error", "Password cannot be empty");
                return "register";
            }
            
            log.info("Attempting to register user: {}", registrationRequest.getUsername());
            userService.registerUser(registrationRequest);
            log.info("User registered successfully: {}", registrationRequest.getUsername());
            
            model.addAttribute("success", "Registration successful. Please login.");
            return "login";
        } catch (Exception e) {
            log.error("Registration failed for username: {}", registrationRequest.getUsername(), e);
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLogin(Model model) {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password, 
                        HttpSession session, Model model) {
        try {
            // Input validation
            if (username == null || username.trim().isEmpty()) {
                log.warn("Login attempt with empty username");
                model.addAttribute("error", "Username is required");
                return "login";
            }
            
            if (password == null || password.trim().isEmpty()) {
                log.warn("Login attempt with empty password for username: {}", username);
                model.addAttribute("error", "Password is required");
                return "login";
            }
            
            log.info("Login attempt for username: {}", username);
            User u = userService.findByUsername(username.trim());
            
            if (u == null) {
                log.warn("Login failed - user not found: {}", username);
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }

            // verify password (BCrypt)
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            
            if (!encoder.matches(password, u.getPassword())) {
                log.warn("Login failed - invalid password for username: {}", username);
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }

            log.info("User logged in successfully: {}", username);
            session.setAttribute("currentUser", u.getUsername());
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Error during login for username: {}", username, e);
            model.addAttribute("error", "An error occurred during login. Please try again.");
            return "login";
        }
    }

    @GetMapping("/welcome")
    public String welcome(HttpSession session, Model model) {
        try {
            Object cu = session.getAttribute("currentUser");
            if (cu == null) {
                log.warn("Unauthorized access attempt to welcome page");
                return "redirect:/login";
            }
            model.addAttribute("username", cu.toString());
            return "welcome";
        } catch (Exception e) {
            log.error("Error accessing welcome page", e);
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        try {
            Object cu = session.getAttribute("currentUser");
            if (cu != null) {
                log.info("User logged out: {}", cu.toString());
            }
            session.invalidate();
        } catch (Exception e) {
            log.error("Error during logout", e);
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/account/delete")
    public String deleteAccount(HttpSession session, Model model) {
        try {
            Object cu = session.getAttribute("currentUser");
            if (cu != null) {
                String username = cu.toString();
                log.info("Attempting to delete account for user: {}", username);
                
                User user = userService.findByUsername(username);
                if (user != null) {
                    userService.deleteUser(user.getId());
                    log.info("Account deleted successfully for user: {}", username);
                }
            }
            session.invalidate();
        } catch (Exception e) {
            log.error("Error deleting account", e);
            model.addAttribute("error", "Failed to delete account. Please try again.");
            return "redirect:/dashboard";
        }
        return "redirect:/dashboard";
    }
}
