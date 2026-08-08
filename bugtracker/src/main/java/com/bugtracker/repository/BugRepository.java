package com.bugtracker.repository;

import com.bugtracker.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Repository
public interface BugRepository extends JpaRepository<Bug, Long>, JpaSpecificationExecutor<Bug> {
    // Finds all bugs with pagination — Spring Data generates the SQL
    Page<Bug> findAll(Pageable pageable);

    /**
     * JPA Specification support — enables dynamic, type-safe queries.
     * We implement Specifications in the next section (7.3).
     * This method signature is provided by JpaSpecificationExecutor
     * which we add to the repository interface below.
     */


    // All bugs in a specific project
    List<Bug> findByProject(Project project);

    // All bugs assigned to a specific developer
    List<Bug> findByAssignee(User assignee);

    // All bugs with a specific status
    List<Bug> findByStatus(BugStatus status);

    // All bugs in a project with a specific status
    List<Bug> findByProjectAndStatus(Project project, BugStatus status);

    // Count bugs by status — used for dashboard statistics
    long countByStatus(BugStatus status);

    // Count bugs assigned to a user with a specific status
    long countByAssigneeAndStatus(User assignee, BugStatus status);

    /**
     * Search bugs by title or description containing a keyword.
     * @Param("keyword") binds the method parameter to :keyword in JPQL.
     */
    @Query("SELECT b FROM Bug b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Bug> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Find bugs for a project ordered by priority then creation date.
     * Used to show the most urgent bugs first.
     */
    @Query("SELECT b FROM Bug b WHERE b.project = :project " +
            "ORDER BY b.priority ASC, b.createdAt DESC")
    List<Bug> findByProjectOrderByPriority(@Param("project") Project project);
}