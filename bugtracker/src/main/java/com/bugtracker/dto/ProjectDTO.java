package com.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Project data.
 *
 * DTOs carry data between the Controller and Service layers.
 * They are NOT database entities — they represent what the user
 * submits via a form or what we send back to the view.
 *
 * We use DTOs to:
 * 1. Avoid exposing internal entity structure to the outside world
 * 2. Add validation annotations specific to the form
 * 3. Decouple the API/UI contract from the database schema
 */
public class ProjectDTO {

    // id is null when creating a new project, populated when editing
    private Long id;

    /**
     * @NotBlank ensures the field is not null and not just whitespace.
     * @Size limits the length of the title.
     * These annotations work with Spring's @Valid in the controller.
     */
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    // --- Constructors ---

    public ProjectDTO() {
        // Default constructor required by Spring for form binding
    }

    public ProjectDTO(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // --- Getters and Setters ---
    // We write these manually now; in Phase 3 we add Lombok to generate them

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "ProjectDTO{id=" + id + ", name='" + name + "', description='" + description + "'}";
    }
}