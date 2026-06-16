package com.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * LoginDTO — carries login form data.
 *
 * Note: Spring Security actually handles the login POST itself
 * via UsernamePasswordAuthenticationFilter. This DTO is used
 * only to bind the form and display it with Thymeleaf.
 * The actual authentication bypasses the controller entirely.
 */
public class LoginDTO {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    // --- Constructors ---
    public LoginDTO() {}

    // --- Getters and Setters ---
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}