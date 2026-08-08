package com.bugtracker.dto;

/**
 * PageRequestDTO — carries pagination and sorting parameters
 * from URL query strings to the service layer.
 *
 * URL example:
 *   /bugs?page=2&size=10&sortBy=priority&sortDir=asc
 *
 * Spring binds these parameters automatically via @ModelAttribute.
 *
 * We use a DTO rather than using Spring's Pageable directly
 * in the controller because:
 *   1. We can set safe defaults (page 0, size 10)
 *   2. We validate the sortBy field against a whitelist
 *      to prevent SQL injection through sort column names
 *   3. The DTO is easier to test and document
 */
public class PageRequestDTO {

    /**
     * Page index. Zero-based — page 0 is the first page.
     * The URL uses page=1 for human readability, so we
     * subtract 1 before passing to Spring Data.
     */
    private int page = 1;

    /**
     * Number of records per page.
     * Default 10; capped at 100 to prevent abuse.
     */
    private int size = 10;

    /**
     * Column to sort by. Must be validated against a whitelist.
     * Default is createdAt (most recently created first).
     */
    private String sortBy = "createdAt";

    /**
     * Sort direction: "asc" or "desc".
     * Default desc — newest items first.
     */
    private String sortDir = "desc";

    // ── Constructors ──
    public PageRequestDTO() {}

    // ── Business methods ──

    /**
     * Returns the zero-based page index for Spring Data.
     * The UI shows page 1, but Spring Data uses page 0.
     */
    public int getZeroBasedPage() {
        return Math.max(0, page - 1);
    }

    /**
     * Returns a safe page size between 1 and 100.
     */
    public int getSafeSize() {
        return Math.min(Math.max(1, size), 100);
    }

    /**
     * Validates the sortBy field against allowed column names.
     * This PREVENTS SQL INJECTION through the sort parameter.
     *
     * If a user sends ?sortBy=; DROP TABLE bugs; -- we return
     * the safe default "createdAt" instead.
     */
    public String getSafeSortBy() {
        return switch (sortBy) {
            case "title", "status", "severity",
                 "priority", "createdAt", "updatedAt"
                    -> sortBy;
            default -> "createdAt";
        };
    }

    /**
     * Returns the sort direction as a Spring Data Sort.Direction.
     */
    public org.springframework.data.domain.Sort.Direction getSortDirection() {
        return "asc".equalsIgnoreCase(sortDir)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
    }

    // ── Getters and Setters ──

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortDir() { return sortDir; }
    public void setSortDir(String sortDir) { this.sortDir = sortDir; }

    /**
     * Returns the opposite sort direction.
     * Used in templates to toggle sort direction when the
     * same column header is clicked again.
     */
    public String getToggleSortDir() {
        return "asc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
    }
}