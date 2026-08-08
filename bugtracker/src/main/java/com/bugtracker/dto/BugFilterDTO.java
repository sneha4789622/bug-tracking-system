package com.bugtracker.dto;

import com.bugtracker.model.BugPriority;
import com.bugtracker.model.BugSeverity;
import com.bugtracker.model.BugStatus;

/**
 * BugFilterDTO — carries search/filter parameters from the URL.
 *
 * When the user selects filters on the bug list page, the form
 * submits GET parameters like:
 *   /bugs?status=NEW&priority=HIGH&projectId=3&keyword=login
 *
 * Spring binds these URL parameters to this object automatically
 * via @ModelAttribute in the controller.
 *
 * All fields are Optional — null means "no filter applied for this field".
 */
public class BugFilterDTO {

    private BugStatus   status;
    private BugPriority priority;
    private BugSeverity severity;
    private Long        projectId;
    private Long        assigneeId;
    private String      keyword;

    // ── Constructors ──
    public BugFilterDTO() {}

    // ── Getters and Setters ──

    public BugStatus getStatus() { return status; }
    public void setStatus(BugStatus status) { this.status = status; }

    public BugPriority getPriority() { return priority; }
    public void setPriority(BugPriority priority) { this.priority = priority; }

    public BugSeverity getSeverity() { return severity; }
    public void setSeverity(BugSeverity severity) { this.severity = severity; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    /**
     * Returns true when at least one filter is active.
     * Used in the template to show/hide a "Clear filters" button.
     */
    public boolean hasActiveFilters() {
        return status     != null
                || priority   != null
                || severity   != null
                || projectId  != null
                || assigneeId != null
                || (keyword   != null && !keyword.isBlank());
    }
}