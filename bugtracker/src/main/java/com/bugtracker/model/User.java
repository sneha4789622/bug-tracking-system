package com.bugtracker.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User Entity — maps to the 'users' table.
 *
 * Represents an application user. In Phase 4, this class will
 * implement Spring Security's UserDetails interface.
 *
 * We use Set<Role> rather than List<Role> for the Many-to-Many
 * relationship because Set prevents duplicate roles and has no
 * ordering concern. Sets also perform better for contains() checks.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * unique = true generates a UNIQUE INDEX on this column in MySQL.
     * This prevents two users from having the same username at the
     * database level — not just the application level.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /**
     * length = 255 because BCrypt hashes are 60 characters,
     * but we use 255 for safety and future algorithm changes.
     * NEVER store plain-text passwords.
     */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * enabled = false means the user cannot log in.
     * Useful for deactivating accounts without deleting them.
     * columnDefinition sets the SQL type/default directly.
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean enabled = true;

    /**
     * @CreationTimestamp tells Hibernate to automatically set this
     * field to the current timestamp when the entity is first saved.
     * updatable = false prevents it from being changed on updates.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Many-to-Many relationship with Role.
     *
     * @ManyToMany — a user can have multiple roles,
     *               a role can belong to multiple users.
     *
     * @JoinTable specifies the join table details:
     *   name = "user_roles"          — the join table name
     *   joinColumns = user_id        — FK from this entity (User)
     *   inverseJoinColumns = role_id — FK to the other entity (Role)
     *
     * FetchType.EAGER means roles are loaded immediately with the user.
     * This is acceptable here because a user typically has 1-2 roles.
     * For large collections, use LAZY loading (the default for collections).
     *
     * CascadeType: We do NOT cascade here because roles are
     * shared across users — deleting a user should not delete ROLE_ADMIN.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // --- Constructors ---

    public User() {}

    public User(String username, String email, String password, String fullName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    // --- Helper Methods ---

    /**
     * Helper to add a role. Keeps the Set management in the entity.
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }
}