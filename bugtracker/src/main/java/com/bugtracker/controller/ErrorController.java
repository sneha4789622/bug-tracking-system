package com.bugtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ErrorController — handles specific HTTP error pages.
 *
 * Spring Boot's default error handling sends all errors to /error.
 * We intercept specific paths here to show friendly, branded pages
 * instead of the default white-label error page.
 *
 * Note: This controller handles URLs we define.
 * Spring Boot's WhitelabelErrorController handles unmatched errors.
 * We override that with our GlobalExceptionHandler.
 */
@Controller
@RequestMapping("/error")
public class ErrorController {

    /**
     * 403 Forbidden — user is authenticated but lacks permission.
     *
     * Common scenario: a DEVELOPER tries to access /admin/users.
     * Spring Security redirects them to /error/403 (configured
     * in SecurityConfig.accessDeniedPage).
     */
    @GetMapping("/403")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Access Denied");
        return "error/403";
    }
}