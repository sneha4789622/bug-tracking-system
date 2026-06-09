package com.bugtracker.model;

/**
 * BugSeverity — the impact level of a bug on the system.
 *
 * Severity is technical: how badly does this break the system?
 * Priority is business: how urgently should this be fixed?
 * They are different! A cosmetic bug in a major feature might
 * have LOW severity but HIGH priority (visible to customers).
 */
public enum BugSeverity {
    CRITICAL("Critical"),   // System crash, data loss
    HIGH("High"),           // Major feature broken
    MEDIUM("Medium"),       // Feature partially broken
    LOW("Low");             // Minor issue, cosmetic

    private final String displayName;

    BugSeverity(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}