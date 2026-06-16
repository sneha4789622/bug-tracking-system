package com.bugtracker.controller;

import com.bugtracker.dto.RegistrationDTO;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController — handles login and registration pages.
 *
 * IMPORTANT: Spring Security intercepts the login POST itself.
 * We only need a GET handler to display the login page.
 * The POST to /auth/login is processed by Spring Security's
 * UsernamePasswordAuthenticationFilter, not by a controller method.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // =========================================================
    // LOGIN
    // =========================================================

    /**
     * Displays the login page.
     *
     * Spring Security adds request parameters for different states:
     *   ?error=true   — login failed (wrong credentials)
     *   ?logout=true  — user just logged out
     *   ?expired=true — session expired due to concurrent login
     *
     * We read these params and pass appropriate messages to the view.
     *
     * @param error    present when login failed
     * @param logout   present when user logged out
     * @param expired  present when session expired
     */
    @GetMapping("/login")
    public String showLoginPage(
            @org.springframework.web.bind.annotation.RequestParam(
                    required = false) String error,
            @org.springframework.web.bind.annotation.RequestParam(
                    required = false) String logout,
            @org.springframework.web.bind.annotation.RequestParam(
                    required = false) String expired,
            Model model) {

        // Pass messages to the template based on URL parameters
        if (error != null) {
            model.addAttribute("errorMessage",
                    "Invalid username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage",
                    "You have been logged out successfully.");
        }
        if (expired != null) {
            model.addAttribute("warningMessage",
                    "Your session expired because you logged in from another location.");
        }

        model.addAttribute("pageTitle", "Login");
        return "auth/login";
    }

    // =========================================================
    // REGISTRATION
    // =========================================================

    /**
     * Displays the registration form with an empty RegistrationDTO.
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDTO", new RegistrationDTO());
        model.addAttribute("pageTitle", "Create Account");
        return "auth/register";
    }

    /**
     * Processes the registration form submission.
     *
     * If @Valid passes, we call the service which may throw
     * IllegalArgumentException for business rule violations
     * (username taken, email taken, passwords don't match).
     * We catch those and add them as form errors.
     */
    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("registrationDTO") RegistrationDTO dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Step 1: Check bean validation (@NotBlank, @Email, @Size)
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Create Account");
            return "auth/register";
        }

        // Step 2: Check business rules in the service
        try {
            userService.registerUser(dto);

            // Success — redirect to login with a success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account created successfully! Please log in.");
            return "redirect:/auth/login";

        } catch (IllegalArgumentException e) {
            // Service threw a business rule violation
            // Add it as a global form error (not tied to a specific field)
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Create Account");
            return "auth/register";
        }
    }
}