package com.bugtracker.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Attachment Entity — maps to the 'attachments' table.
 *
 * Stores metadata about uploaded files.
 * The actual file bytes live on the server filesystem
 * under the configured upload directory.
 */
@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name the user gave the file (what they see).
     * e.g. "screenshot.png"
     */
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    /**
     * The name we store it as on disk.
     * We use a UUID-based name to prevent:
     *   1. Name collisions between users
     *   2. Directory traversal attacks
     *      (user uploads file named "../../etc/passwd")
     */
    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    /**
     * MIME type: image/png, application/pdf, text/plain, etc.
     * Stored so we can set Content-Type when serving the file.
     */
    @Column(name = "content_type", nullable = false)
    private String contentType;

    /**
     * File size in bytes.
     * Used for display and to enforce size limits.
     */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bug_id", nullable = false)
    private Bug bug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // ── Constructors ──
    public Attachment() {}

    public Attachment(String originalFilename, String storedFilename,
                      String contentType, long fileSize,
                      Bug bug, User uploadedBy) {
        this.originalFilename = originalFilename;
        this.storedFilename   = storedFilename;
        this.contentType      = contentType;
        this.fileSize         = fileSize;
        this.bug              = bug;
        this.uploadedBy       = uploadedBy;
    }

    // ── Getters and Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String n) { this.originalFilename = n; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String n) { this.storedFilename = n; }

    public String getContentType() { return contentType; }
    public void setContentType(String t) { this.contentType = t; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long s) { this.fileSize = s; }

    public Bug getBug() { return bug; }
    public void setBug(Bug bug) { this.bug = bug; }

    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User u) { this.uploadedBy = u; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime t) { this.uploadedAt = t; }

    /**
     * Returns file size as a human-readable string.
     * e.g. 1536 → "1.5 KB"
     */
    public String getFormattedSize() {
        if (fileSize < 1024)       return fileSize + " B";
        if (fileSize < 1024 * 1024)
            return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}