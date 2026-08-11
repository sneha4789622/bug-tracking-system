package com.bugtracker.config;

import com.bugtracker.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — the complete Spring Security configuration.
 *
 * @Configuration — Spring processes this class for @Bean definitions
 * @EnableWebSecurity — activates Spring Security's web integration
 * @EnableMethodSecurity — enables @PreAuthorize on individual methods
 *   (e.g. @PreAuthorize("hasRole('ADMIN')") on a service method)
 *
 * CIRCULAR DEPENDENCY NOTE:
 * SecurityConfig needs UserService (for authentication provider).
 * UserService needs PasswordEncoder (to hash passwords).
 * PasswordEncoder is defined as a @Bean in SecurityConfig.
 *
 * If we injected UserService into SecurityConfig directly via
 * constructor injection, Spring would see:
 *   SecurityConfig → UserService → PasswordEncoder → SecurityConfig
 * and throw a circular dependency error.
 *
 * Solution: We use @Lazy injection or define PasswordEncoder
 * as a separate @Bean that UserService can receive independently.
 * Spring resolves this because PasswordEncoder does not depend
 * on UserService — the cycle is broken.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableAsync          // ← add this

public class SecurityConfig {


    /**
     * PasswordEncoder Bean.
     *
     * BCryptPasswordEncoder is the industry standard for password hashing.
     * The strength parameter (10) controls the work factor.
     * Higher = more secure but slower. 10-12 is the recommended range.
     *
     * This is a @Bean so Spring can inject it anywhere — including
     * into UserService — without creating a circular dependency.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * DaoAuthenticationProvider — connects Spring Security to our
     * database-backed user loading system.
     *
     * It uses:
     *   - UserService.loadUserByUsername() to fetch the user
     *   - PasswordEncoder to verify the submitted password
     *     against the stored BCrypt hash
     *
     * @Lazy on UserService breaks the potential circular dependency:
     *   Spring creates SecurityConfig first, creates the Bean methods,
     *   then lazily initializes UserService when first needed.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            @Lazy UserService userService) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;


    }

    /**
     * AuthenticationManager — the central authentication coordinator.
     *
     * We expose this as a @Bean so it can be injected into controllers
     * or services that need to programmatically authenticate a user
     * (e.g. auto-login after registration).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * SecurityFilterChain — defines the security rules for HTTP requests.
     *
     * This is the most important configuration method.
     * It answers three questions:
     *   1. Which URLs are public (no login required)?
     *   2. Which URLs are protected (login required)?
     *   3. How does login and logout work?
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ─── CSRF Configuration ─────────────────────────────
                // CSRF (Cross-Site Request Forgery) protection is enabled
                // by default. Thymeleaf automatically adds the CSRF token
                // to forms via th:action. We keep it ENABLED here (unlike
                // Phase 1's temporary config) for proper security.
                // No action needed — it works automatically with Thymeleaf.

                // ─── URL Authorization Rules ─────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // Public URLs — no login required
                        // These are matched in order — first match wins
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/generate-hash",   // ← add this line

                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",      // ← add this
                                "/favicon.svg",      // ← and this
                                "/webjars/**",
                                "/health"
                        ).permitAll()

                        // Admin-only URLs
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Project Manager and Admin URLs
                        .requestMatchers("/projects/new", "/projects/*/edit",
                                "/projects/*/delete")
                        .hasAnyRole("ADMIN", "PROJECT_MANAGER")

                        // All other URLs require any authenticated user
                        .anyRequest().authenticated()
                )

                // ─── Login Configuration ─────────────────────────────
                .formLogin(login -> login

                        // The URL of our custom login page (GET)
                        .loginPage("/auth/login")

                        // The URL that receives the login form (POST)
                        // Spring Security handles this POST internally —
                        // you do NOT need a controller method for it
                        .loginProcessingUrl("/auth/login")

                        // The form field names — must match your HTML form
                        .usernameParameter("username")
                        .passwordParameter("password")

                        // Where to go after successful login
                        .defaultSuccessUrl("/dashboard", true)

                        // Where to go after failed login
                        // Spring Security appends ?error automatically
                        .failureUrl("/auth/login?error=true")

                        // Make the login page accessible to all
                        .permitAll()
                )

                // ─── Logout Configuration ─────────────────────────────
                .logout(logout -> logout

                        // The URL that triggers logout (POST for security)
                        .logoutUrl("/logout")


                        // After logout, redirect here
                        .logoutSuccessUrl("/auth/login?logout=true")

                        // Invalidate the HTTP session (clear all session data)
                        .invalidateHttpSession(true)

                        // Clear the Spring Security context
                        .clearAuthentication(true)

                        // Delete the remember-me cookie if present
                        .deleteCookies("JSESSIONID")

                        .permitAll()
                )

                // ─── Session Management ───────────────────────────────
                .sessionManagement(session -> session

                        // If a second login happens for the same user,
                        // expire the first session (prevent concurrent logins)
                        .maximumSessions(1)

                        // Where to redirect when session is expired by a
                        // concurrent login
                        .expiredUrl("/auth/login?expired=true")
                )
                // ─── Exception Handling ───────────────────────────────
                .exceptionHandling(ex -> ex

                // Custom 403 page — shown when an authenticated user
                // tries to access a URL their role does not permit.
                // Without this, Spring shows a plain white error page.
                .accessDeniedPage("/error/403")
        );

        return http.build();
    }
}