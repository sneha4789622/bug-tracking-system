package com.bugtracker.service;

import com.bugtracker.dto.ProjectDTO;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.model.Project;
import com.bugtracker.model.ProjectStatus;
import com.bugtracker.model.User;
import com.bugtracker.repository.ProjectRepository;
import com.bugtracker.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ProjectService — updated to use JPA Repository.
 *
 * Key changes from Phase 2:
 * 1. Inject ProjectRepository instead of using an ArrayList
 * 2. Convert between Project entities and ProjectDTOs
 * 3. Add @Transactional to methods that modify data
 *
 * The controller has NOT changed — this demonstrates the value
 * of layered architecture. We swapped the data source without
 * touching the web layer.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * @Transactional(readOnly = true) tells Spring and the database:
     * "This operation only reads data."
     * Benefits:
     *   - Database can optimize with read-only transaction mode
     *   - Hibernate skips dirty-checking (checking if entities changed)
     *   - Slight performance improvement for read-heavy operations
     */
    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::convertToDTO)   // convert each Project to ProjectDTO
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + id));
        return convertToDTO(project);
    }

    /**
     * @Transactional (without readOnly) wraps this method in a
     * database transaction. If any exception occurs, the entire
     * operation is rolled back — no partial saves.
     */
    @Transactional
    public ProjectDTO createProject(ProjectDTO dto) {
        Project project = new Project();
        project.setName(dto.getName().trim());
        project.setDescription(dto.getDescription());
        project.setStatus(ProjectStatus.ACTIVE);

        // Set the creator to the currently logged-in user
        User currentUser = SecurityUtils.getCurrentUser();
        project.setCreatedBy(currentUser);

        Project saved = projectRepository.save(project);
        return convertToDTO(saved);
    }

    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + id));

        project.setName(dto.getName().trim());
        project.setDescription(dto.getDescription());

        // No explicit save() needed — Hibernate detects the change
        // and issues UPDATE automatically when the transaction commits.
        // This is called "dirty checking."
        return convertToDTO(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with ID: " + id);
        }
        projectRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long getProjectCount() {
        return projectRepository.count();
    }

    // =========================================================
    // PRIVATE HELPER: Entity ↔ DTO Conversion
    // =========================================================

    /**
     * Converts a Project entity to a ProjectDTO.
     * This method controls exactly what data leaves the service layer.
     */
    private ProjectDTO convertToDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        return dto;
    }

    /**
     * Converts a ProjectDTO to a Project entity.
     * Used when creating new entities from form data.
     */
    private Project convertToEntity(ProjectDTO dto) {
        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        return project;
    }
}