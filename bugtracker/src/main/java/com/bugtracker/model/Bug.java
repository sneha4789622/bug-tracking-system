package com.bugtracker.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bug Entity — the central entity of the system.
 * Maps to the 'bugs' table.
 *
 * Note two @ManyToOne relationships to User:
 *   reporter — who found and reported the bug
 *   assignee — which developer is responsible for fixing it
 *
 * Both reference the same 'users' table but through different
 * foreign key columns (reporter_id and assignee_id).
 */
@Entity
@Table(name = "bugs")
public class Bug {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BugSeverity severity = BugSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BugPriority priority = BugPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BugStatus status = BugStatus.NEW;

    /**
     * Many bugs belong to one project.
     * This side (Bug) owns the relationship — it holds the
     * 'project_id' foreign key column in the bugs table.
     *
     * optional = false means project_id cannot be NULL.
     * A bug MUST belong to a project.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * Many bugs are reported by one user.
     * optional = true — a bug can exist even if the reporter
     * account is deleted (ON DELETE SET NULL in the schema).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    /**
     * Many bugs are assigned to one developer.
     * nullable — a bug may not be assigned yet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /**
     * One bug has many comments.
     * mappedBy = "bug" — the Comment entity's 'bug' field owns the FK.
     */
    @OneToMany(mappedBy = "bug",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    /**
     * One bug has many history records.
     * Ordered by changedAt descending so latest changes appear first.
     */
    @OneToMany(mappedBy = "bug",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("changedAt DESC")
    private List<BugHistory> history = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Constructors ---

    public Bug() {}

    public Bug(String title, String description, Project project, User reporter) {
        this.title = title;
        this.description = description;
        this.project = project;
        this.reporter = reporter;
    }

    // --- Helper Methods ---

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setBug(this);
    }

    public void addHistory(BugHistory entry) {
        history.add(entry);
        entry.setBug(this);
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BugSeverity getSeverity() { return severity; }
    public void setSeverity(BugSeverity severity) { this.severity = severity; }

    public BugPriority getPriority() { return priority; }
    public void setPriority(BugPriority priority) { this.priority = priority; }

    public BugStatus getStatus() { return status; }
    public void setStatus(BugStatus status) { this.status = status; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }

    public List<BugHistory> getHistory() { return history; }
    public void setHistory(List<BugHistory> history) { this.history = history; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Bug{id=" + id + ", title='" + title + "', status=" + status + "}";
    }
    @OneToMany(mappedBy = "bug",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();
}