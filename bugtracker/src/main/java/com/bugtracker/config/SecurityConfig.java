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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableAsync
public class SecurityConfig {

    /**
     * PasswordEncoder bean — defined first, no dependencies.
     * UserService depends on this bean, NOT the other way around.
     * This breaks the circular dependency completely.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * DaoAuthenticationProvider — uses @Lazy on UserService
     * to break the circular dependency:
     *
     * SecurityConfig creates PasswordEncoder (no deps)
     *         ↓
     * Spring creates UserService (needs PasswordEncoder ✓)
     *         ↓
     * SecurityConfig creates DaoAuthenticationProvider
     *         (needs UserService — now available ✓)
     *
     * @Lazy means: do not inject UserService immediately when
     * SecurityConfig is created. Inject it only when
     * authenticationProvider() is first called.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            @Lazy UserService userService) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // Public URLs — no login required
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/health",
                                "/error/**"
                        ).permitAll()

                        // Admin-only URLs
                        .requestMatchers(
                                "/admin/**"
                        ).hasRole("ADMIN")

                        // Delete operations — Admin and PM only
                        .requestMatchers(
                                "/bugs/*/delete",
                                "/projects/*/delete"
                        ).hasAnyRole("ADMIN", "PROJECT_MANAGER")

                        // Create/edit projects — Admin and PM only
                        .requestMatchers(
                                "/projects/new",
                                "/projects/*/edit"
                        ).hasAnyRole("ADMIN", "PROJECT_MANAGER")

                        // Everything else needs authentication
                        .anyRequest().authenticated()
                )

                .formLogin(login -> login
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutRequestMatcher(
                                new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/auth/login?expired=true")
                )

                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                );

        return http.build();
    }
}