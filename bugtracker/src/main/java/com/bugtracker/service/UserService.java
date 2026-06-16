package com.bugtracker.service;

import com.bugtracker.dto.RegistrationDTO;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.model.Role;
import com.bugtracker.model.User;
import com.bugtracker.repository.RoleRepository;
import com.bugtracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserService — handles user management and implements
 * Spring Security's UserDetailsService.
 *
 * UserDetailsService has one required method:
 *   loadUserByUsername(String username)
 *
 * Spring Security calls this during login to load the user
 * from the database. It then compares the submitted password
 * (hashed) against the stored hash.
 *
 * We implement it here in the service, not the controller,
 * because loading a user is business logic, not web logic.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository    userRepository;
    private final RoleRepository    roleRepository;
    private final PasswordEncoder   passwordEncoder;

    /**
     * Constructor injection.
     * PasswordEncoder is a bean defined in SecurityConfig.
     * Spring automatically injects it here.
     */
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.roleRepository  = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================
    // UserDetailsService Implementation
    // =========================================================

    /**
     * Called by Spring Security during login.
     *
     * Flow:
     *   1. User submits login form (username + password)
     *   2. Spring Security calls this method with the username
     *   3. We load the User from the database
     *   4. Spring Security compares the submitted password
     *      against user.getPassword() using BCrypt
     *   5. If match → authentication success → redirect to dashboard
     *      If no match → authentication failure → back to login with error
     *
     * @throws UsernameNotFoundException if no user found with this username.
     *         Spring Security catches this and shows the login error page.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username: " + username));
    }

    // =========================================================
    // Registration
    // =========================================================

    /**
     * Registers a new user with the DEVELOPER role by default.
     *
     * Business rules enforced here:
     *   1. Username must not already exist
     *   2. Email must not already exist
     *   3. Password and confirmPassword must match
     *   4. Password is BCrypt-hashed before saving
     *   5. New users get ROLE_DEVELOPER by default
     *
     * @param dto the registration form data
     * @throws IllegalArgumentException if any business rule is violated
     */
    @Transactional
    public void registerUser(RegistrationDTO dto) {

        // Business Rule 1: Username must be unique
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException(
                    "Username '" + dto.getUsername() + "' is already taken");
        }

        // Business Rule 2: Email must be unique
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException(
                    "An account with email '" + dto.getEmail() + "' already exists");
        }

        // Business Rule 3: Passwords must match
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Create the User entity
        User user = new User();
        user.setFullName(dto.getFullName().trim());
        user.setUsername(dto.getUsername().trim().toLowerCase());
        user.setEmail(dto.getEmail().trim().toLowerCase());

        // Business Rule 4: Hash the password — NEVER store plain text
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEnabled(true);

        // Business Rule 5: Assign default role
        Role developerRole = roleRepository.findByName("ROLE_DEVELOPER")
                .orElseThrow(() -> new RuntimeException(
                        "ROLE_DEVELOPER not found. Did you run the SQL seed script?"));

        user.addRole(developerRole);

        userRepository.save(user);
    }

    // =========================================================
    // Admin Operations
    // =========================================================

    /**
     * Retrieves all users. Used by admin for the user management page.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Finds a user by ID.
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + id));
    }

    /**
     * Returns all users with the DEVELOPER role.
     * Used when assigning bugs to developers (Phase 5).
     */
    @Transactional(readOnly = true)
    public List<User> getAllDevelopers() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.hasRole("ROLE_DEVELOPER"))
                .toList();
    }

    /**
     * Changes a user's role. Admin-only operation.
     */
    @Transactional
    public void changeUserRole(Long userId, String newRoleName) {
        User user = getUserById(userId);

        Role newRole = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found: " + newRoleName));

        // Clear existing roles and assign the new one
        // In a real app you might allow multiple roles
        user.getRoles().clear();
        user.addRole(newRole);

        // No explicit save() — dirty checking handles the update
    }

    /**
     * Enables or disables a user account. Admin-only.
     */
    @Transactional
    public void setUserEnabled(Long userId, boolean enabled) {
        User user = getUserById(userId);
        user.setEnabled(enabled);
    }

    /**
     * Gets the currently logged-in User entity from the database.
     * Takes the username from Spring Security's context.
     */
    @Transactional(readOnly = true)
    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Current user not found: " + username));
    }
}