package com.bugtracker.controller;

import com.bugtracker.model.User;
import com.bugtracker.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * AdminController — restricted to ROLE_ADMIN.
 *
 * Two layers of security protect these endpoints:
 *
 * Layer 1 (URL-level): SecurityConfig has
 *   .requestMatchers("/admin/**").hasRole("ADMIN")
 *   Any non-admin hitting /admin/* gets redirected to /error/403.
 *
 * Layer 2 (method-level): @PreAuthorize on the class
 *   Even if SecurityConfig is misconfigured, the method
 *   annotation still protects each handler.
 *
 * This "defence in depth" approach is industry best practice.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // =========================================================
    // USER MANAGEMENT LIST
    // GET /admin/users
    // =========================================================

    /**
     * Displays all users with their roles and enabled status.
     * Allows the admin to activate/deactivate accounts and
     * change roles.
     */
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();

        model.addAttribute("users",     users);
        model.addAttribute("pageTitle", "User Management");

        // For the role-change dropdown in the template
        model.addAttribute("availableRoles", List.of(
                "ROLE_ADMIN",
                "ROLE_PROJECT_MANAGER",
                "ROLE_DEVELOPER"
        ));

        return "admin/users";
    }

    // =========================================================
    // CHANGE USER ROLE
    // POST /admin/users/{id}/role
    // =========================================================

    /**
     * Changes a user's role.
     *
     * Business rule applied in UserService:
     *   - Clears all existing roles
     *   - Assigns the single selected role
     *
     * In a more advanced system you might allow multiple roles.
     */
    @PostMapping("/users/{id}/role")
    public String changeRole(
            @PathVariable Long id,
            @RequestParam String roleName,
            RedirectAttributes redirectAttributes) {

        try {
            userService.changeUserRole(id, roleName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Role updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to update role: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    // =========================================================
    // TOGGLE USER ENABLED / DISABLED
    // POST /admin/users/{id}/toggle
    // =========================================================

    /**
     * Activates or deactivates a user account.
     * A disabled user cannot log in (Spring Security checks
     * UserDetails.isEnabled() before authentication).
     *
     * @param enabled "true" to enable, "false" to disable.
     *                Comes from a hidden form field.
     */
    @PostMapping("/users/{id}/toggle")
    public String toggleEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled,
            RedirectAttributes redirectAttributes) {

        userService.setUserEnabled(id, enabled);

        String action = enabled ? "activated" : "deactivated";
        redirectAttributes.addFlashAttribute("successMessage",
                "User account " + action + " successfully.");

        return "redirect:/admin/users";
    }
}