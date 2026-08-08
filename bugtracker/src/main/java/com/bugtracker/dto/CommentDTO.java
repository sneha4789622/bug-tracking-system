package com.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * CommentDTO — carries comment data for both input (form submission)
 * and output (displaying the comment thread on the bug detail page).
 */
public class CommentDTO {

    private Long id;

    @NotBlank(message = "Comment cannot be empty")
    @Size(min = 2, max = 2000,
            message = "Comment must be between 2 and 2000 characters")
    private String content;

    // ── Output fields ──
    private Long          bugId;
    private String        authorName;
    private Long          authorId;
    private LocalDateTime createdAt;

    // ── Constructors ──
    public CommentDTO() {}

    // ── Getters and Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getBugId() { return bugId; }
    public void setBugId(Long bugId) { this.bugId = bugId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}