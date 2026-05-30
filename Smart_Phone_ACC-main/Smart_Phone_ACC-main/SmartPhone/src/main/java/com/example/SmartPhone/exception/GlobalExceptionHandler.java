package com.example.SmartPhone.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public String handleDatabaseException(DataAccessException e, Model model, HttpServletRequest request) {
        log.error("Database error occurred at {}: {}", request.getRequestURI(), e.getMessage(), e);
        model.addAttribute("errorTitle", "Database Error");
        model.addAttribute("errorMessage", "We're experiencing database issues. Please try again later.");
        model.addAttribute("errorDetails", "If the problem persists, please contact support.");
        return "error";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(NoHandlerFoundException e, Model model) {
        log.warn("Page not found: {}", e.getRequestURL());
        model.addAttribute("errorTitle", "Page Not Found");
        model.addAttribute("errorMessage", "The page you're looking for doesn't exist.");
        model.addAttribute("errorDetails", "Please check the URL or return to the dashboard.");
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, Model model, HttpServletRequest request) {
        log.warn("Invalid argument at {}: {}", request.getRequestURI(), e.getMessage());
        model.addAttribute("errorTitle", "Invalid Input");
        model.addAttribute("errorMessage", "The provided input is invalid.");
        model.addAttribute("errorDetails", e.getMessage());
        return "error";
    }

    @ExceptionHandler(NullPointerException.class)
    public String handleNullPointer(NullPointerException e, Model model, HttpServletRequest request) {
        log.error("Null pointer exception at {}: {}", request.getRequestURI(), e.getMessage(), e);
        model.addAttribute("errorTitle", "System Error");
        model.addAttribute("errorMessage", "An unexpected error occurred.");
        model.addAttribute("errorDetails", "Our team has been notified. Please try again.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception e, Model model, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), e.getMessage(), e);
        model.addAttribute("errorTitle", "Unexpected Error");
        model.addAttribute("errorMessage", "Something went wrong on our end.");
        model.addAttribute("errorDetails", "Please try again. If the issue continues, contact support.");
        return "error";
    }
}
