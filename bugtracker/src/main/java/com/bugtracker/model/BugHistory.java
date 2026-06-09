package com.bugtracker.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * BugHistory Entity — maps to the 'bug_history' table.
 *
 * Every time a bug's field changes (status, assignee, priority, etc.),
 * we insert a row here. This creates a full audit trail showing
 * exactly what changed, when, and who made the change.
 *
 * This is an immutable audit log — records are only ever inserted,
 * never updated or deleted (except when the parent bug is deleted
 * via CASCADE).
 */
@Entity
@Table(name = "bug_history")
public class BugHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bug_id", nullable = false)
    private Bug bug;

    /**
     * Who made the change. Nullable because the user account
     * might be deleted after making the change.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    /**
     * Which field was changed, e.g. "status", "assignee", "priority".
     */
    @Column(name = "field_changed", nullable = false, length = 50)
    private String fieldChanged;

    /**
     * The value before the change.
     * e.g. "NEW" if status changed from NEW to IN_PROGRESS.
     */
    @Column(name = "old_value", length = 255)
    private String oldValue;

    /**
     * The value after the change.
     * e.g. "IN_PROGRESS".
     */
    @Column(name = "new_value", length = 255)
    private String newValue;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    // --- Constructors ---

    public BugHistory() {}

    /**
     * Convenience constructor for creating history entries.
     */
    public BugHistory(Bug bug, User changedBy,
                      String fieldChanged, String oldValue, String newValue) {
        this.bug = bug;
        this.changedBy = changedBy;
        this.fieldChanged = fieldChanged;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Bug getBug() { return bug; }
    public void setBug(Bug bug) { this.bug = bug; }

    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }

    public String getFieldChanged() { return fieldChanged; }
    public void setFieldChanged(String fieldChanged) { this.fieldChanged = fieldChanged; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    @Override
    public String toString() {
        return "BugHistory{field='" + fieldChanged +
                "', '" + oldValue + "' → '" + newValue + "'}";
    }
}