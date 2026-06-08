package com.bugtracker.controller;

import com.bugtracker.service.ProjectService;
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
    private final ProjectService projectService;
    public HomeController (ProjectService projectService){
        this.projectService = projectService;
    }
    @GetMapping("/")
    public String home() {
        // Redirect root URL to /dashboard
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Fetch summary statistics for the dashboard
        model.addAttribute("totalProjects", projectService.getProjectCount());
        model.addAttribute("pageTitle", "Dashboard");

        // Placeholder values — will come from real services in Phase 3
        model.addAttribute("totalBugs", 0);
        model.addAttribute("openBugs", 0);
        model.addAttribute("resolvedBugs", 0);

        return "dashboard";
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
