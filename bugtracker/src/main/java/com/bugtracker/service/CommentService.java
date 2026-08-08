package com.bugtracker.service;

import com.bugtracker.dto.CommentDTO;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.model.Bug;
import com.bugtracker.model.Comment;
import com.bugtracker.repository.BugRepository;
import com.bugtracker.repository.CommentRepository;
import com.bugtracker.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CommentService — handles all comment operations.
 *
 * Kept separate from BugService because:
 *   1. Single Responsibility Principle
 *   2. Comments have their own lifecycle (add, display)
 *   3. Easier to test independently
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final BugRepository     bugRepository;

    public CommentService(CommentRepository commentRepository,
                          BugRepository     bugRepository) {
        this.commentRepository = commentRepository;
        this.bugRepository     = bugRepository;
    }

    /**
     * Returns all comments for a bug, ordered chronologically
     * (oldest first, so the thread reads naturally top-to-bottom).
     */
    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsForBug(Long bugId) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bug not found with ID: " + bugId));

        return commentRepository.findByBugOrderByCreatedAtAsc(bug)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Adds a new comment to a bug.
     *
     * Business rules:
     *   1. Bug must exist
     *   2. Author is always the currently logged-in user
     *   3. Content must not be blank (validated by @Valid in controller)
     */
    @Transactional
    public CommentDTO addComment(Long bugId, CommentDTO dto) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bug not found with ID: " + bugId));

        Comment comment = new Comment(
                dto.getContent().trim(),
                bug,
                SecurityUtils.getCurrentUser()
        );

        Comment saved = commentRepository.save(comment);
        return convertToDTO(saved);
    }

    /**
     * Deletes a comment. A user can only delete their own comments
     * unless they are an Admin.
     */
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comment not found with ID: " + commentId));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isOwner = comment.getUser() != null
                && comment.getUser().getId().equals(currentUserId);

        if (!isOwner && !SecurityUtils.isAdmin()) {
            throw new SecurityException(
                    "You do not have permission to delete this comment");
        }

        commentRepository.delete(comment);
    }

    // ── Private Helpers ──

    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setBugId(comment.getBug().getId());
        dto.setCreatedAt(comment.getCreatedAt());

        if (comment.getUser() != null) {
            dto.setAuthorName(comment.getUser().getFullName());
            dto.setAuthorId(comment.getUser().getId());
        } else {
            dto.setAuthorName("Deleted User");
        }

        return dto;
    }
}