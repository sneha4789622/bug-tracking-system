package com.bugtracker.model;

/**
 * BugStatus — the workflow state of a bug.
 *
 * Valid transitions:
 * NEW → IN_PROGRESS → TESTING → RESOLVED → CLOSED
 *             ↑                      |
 *             └──────────────────────┘ (reopened)
 *
 * We enforce these transitions in the BugService layer,
 * not in the enum itself, because business rules belong
 * in the service.
 */
public enum BugStatus {
    NEW("New"),
    IN_PROGRESS("In Progress"),
    TESTING("Testing"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    private final String displayName;

    BugStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}