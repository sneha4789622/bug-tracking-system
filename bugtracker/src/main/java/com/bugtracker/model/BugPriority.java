package com.bugtracker.model;

/**
 * BugPriority — business urgency for fixing a bug.
 */
public enum BugPriority {
    CRITICAL("Critical"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    private final String displayName;

    BugPriority(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}