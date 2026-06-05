package com.bugtracker.bugtracker.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home Controller - handles requests to the root URL.
 * This is our first controller to verify the application works.
 */
@Controller
public class HomeController {

    /**
     * Handles GET requests to the root URL (/).
     *
     * @param model The Model object to pass data to the view
     * @return The name of the Thymeleaf template to render
     */
    @GetMapping("/")
    public String home(Model model) {
        // Add data to the model - this will be available in the template
        model.addAttribute("title", "Bug Tracking System");
        model.addAttribute("message", "Welcome to the Bug Tracking System!");

        // Return the template name (templates/home.html)
        return "home";
    }

    /**
     * Health check endpoint for monitoring.
     * Returns a simple text response.
     */
    @GetMapping("/health")
    @org.springframework.web.bind.annotation.ResponseBody
    public String health() {
        return "Application is running!";
    }
}
