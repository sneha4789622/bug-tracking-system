package com.bugtracker.model;

import jakarta.persistence.*;

/**
 * Role Entity — maps to the 'roles' table.
 *
 * Represents an authority level in the system:
 * ROLE_ADMIN, ROLE_PROJECT_MANAGER, ROLE_DEVELOPER.
 *
 * The "ROLE_" prefix is required by Spring Security.
 * When you call hasRole("ADMIN"), Spring internally checks
 * for "ROLE_ADMIN" in the user's authorities.
 */
@Entity
@Table(name = "roles")
public class Role {

    /**
     * @Id marks this as the primary key.
     * @GeneratedValue(strategy = GenerationType.IDENTITY) tells
     * Hibernate to use the database's AUTO_INCREMENT column.
     * Other strategies: SEQUENCE (Oracle), TABLE (portable but slow),
     * AUTO (Hibernate decides).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @Column(unique = true) enforces uniqueness at the DB level.
     * nullable = false maps to NOT NULL.
     * length = 50 sets VARCHAR(50).
     */
    @Column(unique = true, nullable = false, length = 50)
    private String name;

    // --- Constructors ---

    public Role() {
        // JPA requires a no-argument constructor
    }

    public Role(String name) {
        this.name = name;
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Role{id=" + id + ", name='" + name + "'}";
    }
}