package com.bugtracker.model;

/**
 * ProjectStatus — represents the lifecycle state of a project.
 *
 * Using an enum instead of a plain String gives us:
 * 1. Compile-time safety — can't accidentally set status = "ACTVE" (typo)
 * 2. IDE autocompletion
 * 3. Exhaustive switch statements
 * 4. Clear domain model documentation
 */
public enum ProjectStatus {

    ACTIVE("Active"),
    ON_HOLD("On Hold"),
    COMPLETED("Completed"),
    ARCHIVED("Archived");

    /**
     * displayName is the human-readable label shown in the UI.
     * The enum name (ACTIVE) is stored in the database.
     */
    private final String displayName;

    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}