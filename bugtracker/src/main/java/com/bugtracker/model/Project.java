package com.bugtracker.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Project Entity — maps to the 'projects' table.
 *
 * A Project groups related bugs together. Every bug belongs
 * to exactly one project.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * @Column(columnDefinition = "TEXT") maps to MySQL's TEXT type,
     * which holds up to 65,535 characters — suitable for long descriptions.
     * Without this, JPA defaults to VARCHAR(255).
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * @Enumerated(EnumType.STRING) stores the enum name as a string
     * in the database (e.g., "ACTIVE") rather than its ordinal (0, 1, 2).
     *
     * ALWAYS use EnumType.STRING. Using EnumType.ORDINAL means adding
     * a new enum value in the middle of the list changes all existing
     * database records — a catastrophic bug.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    /**
     * Many bugs belong to one project (from Bug's perspective).
     * One project has many bugs (from Project's perspective).
     *
     * @ManyToOne — the "many" side holds the foreign key.
     * @JoinColumn(name = "created_by") — the FK column name in the projects table.
     * FetchType.LAZY — do NOT load the User unless explicitly accessed.
     *                  This is the default and the correct choice for
     *                  @ManyToOne relationships in most cases.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * One Project → Many Bugs.
     *
     * mappedBy = "project" means the 'project' field in the Bug entity
     * owns this relationship (holds the foreign key). The Project side
     * is the "inverse" side and does not own the FK.
     *
     * CascadeType.ALL — any operation on Project cascades to its bugs:
     *   save project → saves bugs, delete project → deletes all its bugs.
     *
     * orphanRemoval = true — if a bug is removed from this list,
     *   it is deleted from the database. This is for child entities
     *   that cannot exist without their parent.
     *
     * FetchType.LAZY — do NOT load all bugs when loading a project.
     *   Only load them when bugs are explicitly accessed. This is
     *   critical for performance with large datasets.
     */
    @OneToMany(mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Bug> bugs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Constructors ---

    public Project() {}

    public Project(String name, String description, User createdBy) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    // --- Helper Methods ---

    public void addBug(Bug bug) {
        bugs.add(bug);
        bug.setProject(this);   // keep both sides of the relationship in sync
    }

    public void removeBug(Bug bug) {
        bugs.remove(bug);
        bug.setProject(null);
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public List<Bug> getBugs() { return bugs; }
    public void setBugs(List<Bug> bugs) { this.bugs = bugs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + "', status=" + status + "}";
    }
}