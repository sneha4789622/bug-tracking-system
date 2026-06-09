package com.bugtracker.repository;

import com.bugtracker.model.Bug;
import com.bugtracker.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // All comments for a bug, ordered oldest first (chronological thread)
    List<Comment> findByBugOrderByCreatedAtAsc(Bug bug);

    // Count comments on a bug
    long countByBug(Bug bug);
}