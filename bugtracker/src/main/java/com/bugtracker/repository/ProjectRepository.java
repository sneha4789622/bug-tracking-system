package com.bugtracker.repository;

import com.bugtracker.model.Project;
import com.bugtracker.model.ProjectStatus;
import com.bugtracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Finds all projects with a given status
    List<Project> findByStatus(ProjectStatus status);

    // Finds all projects created by a specific user
    List<Project> findByCreatedBy(User user);

    // Finds projects whose name contains the search string (case-insensitive)
    // Generated SQL: WHERE LOWER(name) LIKE LOWER(CONCAT('%', ?, '%'))
    List<Project> findByNameContainingIgnoreCase(String name);

    /**
     * @Query lets us write JPQL (Java Persistence Query Language)
     * when the method naming convention is too complex.
     *
     * JPQL uses entity class names and field names, NOT table/column names.
     * "p" is an alias for Project. "p.status" refers to the status field.
     *
     * This is equivalent to:
     * SELECT * FROM projects WHERE status != 'ARCHIVED' ORDER BY created_at DESC
     */
    @Query("SELECT p FROM Project p WHERE p.status != 'ARCHIVED' ORDER BY p.createdAt DESC")
    List<Project> findAllActiveProjects();
}