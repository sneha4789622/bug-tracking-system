package com.bugtracker.dto;

import com.bugtracker.model.BugPriority;
import com.bugtracker.model.BugSeverity;
import com.bugtracker.model.BugStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * BugDTO — carries bug data between the web layer and service layer.
 *
 * Contains two categories of fields:
 *
 * 1. INPUT fields — submitted from forms (title, description, etc.)
 *    These have validation annotations.
 *
 * 2. OUTPUT fields — populated by the service for display (reporter name,
 *    assignee name, project name, formatted dates).
 *    These do NOT have validation annotations.
 *
 * Keeping both in one DTO is acceptable for a project this size.
 * In larger systems you would have separate CreateBugDTO and BugResponseDTO.
 */
public class BugDTO {

    // ── Output field (no validation needed) ──
    private Long id;

    // ── Input field ──
    @NotBlank(message = "Bug title is required")
    @Size(min = 5, max = 200,
            message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, message = "Description must be at least 10 characters")
    private String description;

    @NotNull(message = "Severity is required")
    private BugSeverity severity;

    @NotNull(message = "Priority is required")
    private BugPriority priority;

    // ── Status is set by the system, not chosen on the create form ──
    private BugStatus status;

    @NotNull(message = "Project is required")
    private Long projectId;

    // ── Output: project name for display ──
    private String projectName;

    // ── Optional on create — bug may be unassigned initially ──
    private Long assigneeId;

    // ── Output fields for display ──
    private String assigneeName;
    private String reporterName;
    private Long   reporterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int    commentCount;

    // ── Constructors ──
    public BugDTO() {}

    // ── Getters and Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public BugSeverity getSeverity() { return severity; }
    public void setSeverity(BugSeverity severity) { this.severity = severity; }

    public BugPriority getPriority() { return priority; }
    public void setPriority(BugPriority priority) { this.priority = priority; }

    public BugStatus getStatus() { return status; }
    public void setStatus(BugStatus status) { this.status = status; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }
}