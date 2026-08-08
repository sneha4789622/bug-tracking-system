package com.bugtracker.util;

import com.bugtracker.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityUtils — static helpers for accessing the current user.
 *
 * We use static methods here because this utility is needed
 * in service methods that do not have access to the HTTP request
 * (unlike controllers, which get @AuthenticationPrincipal).
 *
 * Static security utilities are a common pattern in Spring Boot
 * applications. The SecurityContextHolder is thread-local, so
 * each request sees its own authentication data safely.
 */
public final class SecurityUtils {

    // Prevent instantiation — this is a utility class
    private SecurityUtils() {}

    /**
     * Returns the currently authenticated User entity.
     *
     * Since our User class implements UserDetails, Spring Security
     * stores it directly in the Authentication principal.
     * We cast it back to User to access all entity fields.
     *
     * @return the logged-in User, or null if not authenticated
     */
    public static User getCurrentUser() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();

        // principal is our User entity (implements UserDetails)
        if (principal instanceof User) {
            return (User) principal;
        }

        return null;
    }

    /**
     * Returns the current user's ID, or null if not authenticated.
     */
    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Returns the current user's username, or null if not authenticated.
     */
    public static String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param roleName e.g. "ROLE_ADMIN"
     */
    public static boolean hasRole(String roleName) {
        User user = getCurrentUser();
        return user != null && user.hasRole(roleName);
    }

    /**
     * Checks if the current user is an Admin.
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Checks if the current user is a Project Manager.
     */
    public static boolean isProjectManager() {
        return hasRole("ROLE_PROJECT_MANAGER");
    }

    /**
     * Checks if the current user is an Admin or Project Manager.
     * These two roles share many permissions.
     */
    public static boolean isAdminOrPM() {
        return isAdmin() || isProjectManager();
    }
}