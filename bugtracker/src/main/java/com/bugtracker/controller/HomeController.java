package com.bugtracker.controller;

import com.bugtracker.model.User;
import com.bugtracker.service.ProjectService;
import com.bugtracker.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * HomeController — dashboard and root URL.
 *
 * @AuthenticationPrincipal injects the currently logged-in User object
 * directly into controller methods. Spring Security stores the
 * UserDetails object (our User entity) in the SecurityContextHolder,
 * and this annotation retrieves it.
 */
@Controller
public class HomeController {

    private final ProjectService projectService;
    private final UserService    userService;

    public HomeController(ProjectService projectService,
                          UserService userService) {
        this.projectService = projectService;
        this.userService    = userService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal User currentUser,
            Model model) {

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("totalProjects", projectService.getProjectCount());

        // Placeholders — replaced in Phase 5 with real bug data
        model.addAttribute("totalBugs",    0);
        model.addAttribute("openBugs",     0);
        model.addAttribute("resolvedBugs", 0);

        return "dashboard";
    }
    @GetMapping("/generate-hash")
    @org.springframework.web.bind.annotation.ResponseBody
    public String generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("admin123");
        return "Hash: " + hash;
    }
    @GetMapping("/health")
    @org.springframework.web.bind.annotation.ResponseBody
    public String health() {
        return "Application is running!";
    }
}