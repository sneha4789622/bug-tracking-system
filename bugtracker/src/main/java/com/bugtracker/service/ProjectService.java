package com.bugtracker.service;

import com.bugtracker.dto.ProjectDTO;
import com.bugtracker.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ProjectService - Business Logic Layer
 *
 * This service handles all business operations for Projects.
 * Currently uses in-memory storage (a List) to demonstrate
 * the layer architecture clearly before we add a real database
 * in Phase 3. The controller will not change when we swap
 * in-memory storage for a real repository — that is the
 * power of layered architecture.
 *
 * @Service tells Spring to:
 *   1. Create an instance of this class (make it a Bean)
 *   2. Make it available for injection into controllers
 *   3. Apply transaction management support (relevant in Phase 3)
 */
@Service
public class ProjectService {

    /**
     * Temporary in-memory storage.
     * We replace this with a real JPA Repository in Phase 3.
     * The controller code will not need to change at all.
     */
    private final List<ProjectDTO> projectStorage = new ArrayList<>();

    /**
     * AtomicLong is thread-safe — multiple requests hitting the
     * server simultaneously won't generate duplicate IDs.
     */
    private final AtomicLong idCounter = new AtomicLong(1);

    /**
     * Retrieves all projects.
     * Business rule: returns an empty list if no projects exist,
     * never returns null (null causes NullPointerExceptions in views).
     *
     * @return List of all projects, never null
     */
    public List<ProjectDTO> getAllProjects() {
        return new ArrayList<>(projectStorage); // return a copy, not the internal list
    }

    /**
     * Finds a project by its ID.
     * Business rule: if not found, throw a specific exception
     * rather than returning null. This makes error handling explicit.
     *
     * @param id the project ID to search for
     * @return the found ProjectDTO
     * @throws ResourceNotFoundException if no project has this ID
     */
    public ProjectDTO getProjectById(Long id) {
        return projectStorage.stream()
                .filter(project -> project.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + id));
    }

    /**
     * Creates a new project.
     * Business rule: assigns a unique ID before saving.
     *
     * @param projectDTO the data submitted from the form
     * @return the saved ProjectDTO with its assigned ID
     */
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        // Assign a new unique ID
        projectDTO.setId(idCounter.getAndIncrement());

        // Business rule: trim whitespace from the name
        projectDTO.setName(projectDTO.getName().trim());

        projectStorage.add(projectDTO);
        return projectDTO;
    }

    /**
     * Updates an existing project.
     * Business rule: only the name and description can be updated.
     * The ID cannot change.
     *
     * @param id         the ID of the project to update
     * @param projectDTO the new data to apply
     * @return the updated ProjectDTO
     */
    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        // First verify the project exists (throws exception if not)
        ProjectDTO existing = getProjectById(id);

        // Apply updates — only allow changing name and description
        existing.setName(projectDTO.getName().trim());
        existing.setDescription(projectDTO.getDescription());

        return existing;
    }

    /**
     * Deletes a project by ID.
     * Business rule: verifies existence before attempting deletion.
     *
     * @param id the ID of the project to delete
     */
    public void deleteProject(Long id) {
        // Verify it exists first — throws ResourceNotFoundException if not
        ProjectDTO existing = getProjectById(id);
        projectStorage.remove(existing);
    }

    /**
     * Returns the total count of projects.
     * Used by the dashboard.
     *
     * @return number of projects
     */
    public long getProjectCount() {
        return projectStorage.size();
    }
}