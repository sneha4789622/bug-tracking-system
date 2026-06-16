package com.bugtracker.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User Entity — now implements Spring Security's UserDetails.
 *
 * UserDetails is the contract Spring Security uses to represent
 * an authenticated user. By implementing it directly on our entity,
 * we avoid creating a separate wrapper class.
 *
 * Methods we must implement:
 *   getAuthorities()     — returns the user's roles/permissions
 *   getPassword()        — returns the stored (hashed) password
 *   getUsername()        — returns the unique identifier (username)
 *   isAccountNonExpired()    — can the account still be used?
 *   isAccountNonLocked()     — is the account locked?
 *   isCredentialsNonExpired()— is the password still valid?
 *   isEnabled()              — is the account active?
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // =========================================================
    // UserDetails Interface Implementation
    // =========================================================

    /**
     * Converts our Role objects into Spring Security's
     * GrantedAuthority objects.
     *
     * Spring Security checks authorities (not Role entities)
     * when deciding if a user can access a resource.
     *
     * Role.getName() returns "ROLE_ADMIN", "ROLE_DEVELOPER", etc.
     * SimpleGrantedAuthority wraps the string into a GrantedAuthority.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Spring Security calls this to get the password for comparison.
     * Returns the BCrypt hash stored in the database.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Spring Security uses this as the unique identifier.
     * Must match what the user types in the login form's
     * username field.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * We return true for all three account state checks.
     * In a production system you might implement account expiry
     * or lockout after failed login attempts.
     * For now, only 'enabled' matters.
     */
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /**
     * Returns our 'enabled' field.
     * If false, Spring Security rejects the login attempt
     * with DisabledException.
     */
    @Override
    public boolean isEnabled() { return enabled; }

    // =========================================================
    // Constructors
    // =========================================================

    public User() {}

    public User(String username, String email,
                String password, String fullName) {
        this.username = username;
        this.email    = email;
        this.password = password;
        this.fullName = fullName;
    }

    // =========================================================
    // Helper Methods
    // =========================================================

    public void addRole(Role role) { this.roles.add(role); }
    public void removeRole(Role role) { this.roles.remove(role); }

    /**
     * Convenience method used in templates.
     * Checks if this user has a specific role name.
     * e.g. user.hasRole("ROLE_ADMIN")
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    // =========================================================
    // Getters and Setters
    // =========================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // Note: setUsername shadows UserDetails.getUsername()
    // This is intentional — JPA needs setters, Spring Security needs getters
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // setPassword — Spring Security reads via getPassword() above
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    @Override
    public String toString() {
        return "User{id=" + id +
                ", username='" + username + "'" +
                ", email='" + email + "'}";
    }
}