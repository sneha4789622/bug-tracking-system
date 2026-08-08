package com.bugtracker.service;

import com.bugtracker.dto.BugDTO;
import com.bugtracker.dto.BugFilterDTO;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.model.*;
import com.bugtracker.repository.*;
import com.bugtracker.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BugService — all business logic for bug operations.
 *
 * Key responsibilities:
 *   1. CRUD operations on Bug entities
 *   2. Status transition validation
 *   3. Bug assignment logic
 *   4. Automatic BugHistory recording on every change
 *   5. Filtering and search
 *   6. DTO conversion
 */
@Service
public class BugService {

    private final BugRepository        bugRepository;
    private final ProjectRepository    projectRepository;
    private final UserRepository       userRepository;
    private final BugHistoryRepository bugHistoryRepository;
    private final CommentRepository    commentRepository;

    public BugService(BugRepository        bugRepository,
                      ProjectRepository    projectRepository,
                      UserRepository       userRepository,
                      BugHistoryRepository bugHistoryRepository,
                      CommentRepository    commentRepository) {
        this.bugRepository        = bugRepository;
        this.projectRepository    = projectRepository;
        this.userRepository       = userRepository;
        this.bugHistoryRepository = bugHistoryRepository;
        this.commentRepository    = commentRepository;
    }

    // =========================================================
    // READ OPERATIONS
    // =========================================================

    /**
     * Returns all bugs, converted to DTOs.
     * Used on the bug list page when no filters are active.
     */
    @Transactional(readOnly = true)
    public List<BugDTO> getAllBugs() {
        return bugRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Applies filters from BugFilterDTO and returns matching bugs.
     *
     * We load all bugs and filter in memory for now.
     * In Phase 7 (Advanced Features) we replace this with
     * JPA Specifications for true database-level filtering.
     */
    @Transactional(readOnly = true)
    public List<BugDTO> getFilteredBugs(BugFilterDTO filter) {

        // Start with all bugs
        List<Bug> bugs = bugRepository.findAll();

        // Apply each filter only if it is not null
        if (filter.getStatus() != null) {
            bugs = bugs.stream()
                    .filter(b -> b.getStatus() == filter.getStatus())
                    .collect(Collectors.toList());
        }

        if (filter.getPriority() != null) {
            bugs = bugs.stream()
                    .filter(b -> b.getPriority() == filter.getPriority())
                    .collect(Collectors.toList());
        }

        if (filter.getSeverity() != null) {
            bugs = bugs.stream()
                    .filter(b -> b.getSeverity() == filter.getSeverity())
                    .collect(Collectors.toList());
        }

        if (filter.getProjectId() != null) {
            bugs = bugs.stream()
                    .filter(b -> b.getProject().getId()
                            .equals(filter.getProjectId()))
                    .collect(Collectors.toList());
        }

        if (filter.getAssigneeId() != null) {
            bugs = bugs.stream()
                    .filter(b -> b.getAssignee() != null
                            && b.getAssignee().getId()
                            .equals(filter.getAssigneeId()))
                    .collect(Collectors.toList());
        }

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            String keyword = filter.getKeyword().toLowerCase();
            bugs = bugs.stream()
                    .filter(b ->
                            b.getTitle().toLowerCase().contains(keyword)
                                    || b.getDescription().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        return bugs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Fetches a single bug by ID.
     * Throws ResourceNotFoundException if not found —
     * caught by GlobalExceptionHandler.
     */
    @Transactional(readOnly = true)
    public BugDTO getBugById(Long id) {
        Bug bug = findBugOrThrow(id);
        return convertToDTO(bug);
    }

    /**
     * Returns all bugs assigned to the currently logged-in developer.
     * Used on the developer dashboard.
     */
    @Transactional(readOnly = true)
    public List<BugDTO> getMyAssignedBugs() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) return List.of();

        return bugRepository.findByAssignee(currentUser)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // CREATE
    // =========================================================

    /**
     * Creates a new bug.
     *
     * Business rules:
     *   1. Project must exist
     *   2. Reporter is always the currently logged-in user
     *   3. Initial status is always NEW (not chosen by user)
     *   4. Assignee is optional on creation
     *   5. Record creation in BugHistory
     */
    @Transactional
    public BugDTO createBug(BugDTO dto) {

        // Rule 1: Project must exist
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + dto.getProjectId()));

        // Rule 2: Reporter = current logged-in user
        User reporter = SecurityUtils.getCurrentUser();

        // Build the Bug entity
        Bug bug = new Bug();
        bug.setTitle(dto.getTitle().trim());
        bug.setDescription(dto.getDescription().trim());
        bug.setSeverity(dto.getSeverity());
        bug.setPriority(dto.getPriority());
        bug.setStatus(BugStatus.NEW);         // Rule 3: always starts as NEW
        bug.setProject(project);
        bug.setReporter(reporter);

        // Rule 4: Optional assignee
        if (dto.getAssigneeId() != null) {
            User assignee = userRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with ID: " + dto.getAssigneeId()));
            bug.setAssignee(assignee);
        }

        Bug saved = bugRepository.save(bug);

        // Rule 5: Record creation in history
        recordHistory(saved, reporter, "status",
                null, BugStatus.NEW.name());

        return convertToDTO(saved);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    /**
     * Updates bug fields (title, description, severity, priority).
     *
     * Business rule: Status and assignment are updated via
     * dedicated methods (updateStatus, assignBug) to ensure
     * the history trail captures each change separately.
     */
    @Transactional
    public BugDTO updateBug(Long id, BugDTO dto) {

        Bug bug         = findBugOrThrow(id);
        User currentUser = SecurityUtils.getCurrentUser();

        // Track changes for history
        if (!bug.getSeverity().equals(dto.getSeverity())) {
            recordHistory(bug, currentUser,
                    "severity",
                    bug.getSeverity().name(),
                    dto.getSeverity().name());
            bug.setSeverity(dto.getSeverity());
        }

        if (!bug.getPriority().equals(dto.getPriority())) {
            recordHistory(bug, currentUser,
                    "priority",
                    bug.getPriority().name(),
                    dto.getPriority().name());
            bug.setPriority(dto.getPriority());
        }

        if (!bug.getTitle().equals(dto.getTitle().trim())) {
            recordHistory(bug, currentUser,
                    "title",
                    bug.getTitle(),
                    dto.getTitle().trim());
            bug.setTitle(dto.getTitle().trim());
        }

        bug.setDescription(dto.getDescription().trim());

        // Dirty checking handles the UPDATE — no explicit save() needed
        return convertToDTO(bug);
    }

    // =========================================================
    // STATUS WORKFLOW
    // =========================================================

    /**
     * Updates the bug status, enforcing valid transitions.
     *
     * Valid transition map:
     *   NEW         → IN_PROGRESS
     *   IN_PROGRESS → TESTING, NEW (reopen)
     *   TESTING     → RESOLVED, IN_PROGRESS (failed testing)
     *   RESOLVED    → CLOSED, IN_PROGRESS (reopen)
     *   CLOSED      → IN_PROGRESS (reopen)
     *
     * Admins and PMs can make any transition.
     * Developers can only transition bugs assigned to them.
     */
    @Transactional
    public BugDTO updateStatus(Long bugId, BugStatus newStatus) {

        Bug bug         = findBugOrThrow(bugId);
        User currentUser = SecurityUtils.getCurrentUser();
        BugStatus oldStatus = bug.getStatus();

        // Validate the transition
        validateStatusTransition(oldStatus, newStatus);

        bug.setStatus(newStatus);

        // Record the status change in history
        recordHistory(bug, currentUser,
                "status", oldStatus.name(), newStatus.name());

        return convertToDTO(bug);
    }

    /**
     * Validates that the requested status transition is permitted.
     *
     * @throws IllegalStateException if the transition is invalid.
     *         GlobalExceptionHandler renders the error page.
     */
    private void validateStatusTransition(BugStatus from, BugStatus to) {

        // Admins and PMs bypass transition rules
        if (SecurityUtils.isAdminOrPM()) return;

        boolean valid = switch (from) {
            case NEW         -> to == BugStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == BugStatus.TESTING
                    || to == BugStatus.NEW;
            case TESTING     -> to == BugStatus.RESOLVED
                    || to == BugStatus.IN_PROGRESS;
            case RESOLVED    -> to == BugStatus.CLOSED
                    || to == BugStatus.IN_PROGRESS;
            case CLOSED      -> to == BugStatus.IN_PROGRESS;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Cannot transition bug from "
                            + from.getDisplayName()
                            + " to "
                            + to.getDisplayName());
        }
    }

    // =========================================================
    // ASSIGNMENT
    // =========================================================

    /**
     * Assigns a bug to a developer.
     * Admin and PM only (enforced at controller URL level).
     *
     * Business rules:
     *   1. Assignee must be an existing user
     *   2. If reassigning, record the old assignee in history
     *   3. If unassigning (assigneeId = null), clear the field
     */
    @Transactional
    public BugDTO assignBug(Long bugId, Long assigneeId) {

        Bug bug         = findBugOrThrow(bugId);
        User currentUser = SecurityUtils.getCurrentUser();

        String oldAssigneeName = bug.getAssignee() != null
                ? bug.getAssignee().getUsername() : "Unassigned";

        if (assigneeId == null) {
            // Unassign
            bug.setAssignee(null);
            recordHistory(bug, currentUser,
                    "assignee", oldAssigneeName, "Unassigned");
        } else {
            User newAssignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with ID: " + assigneeId));
            bug.setAssignee(newAssignee);
            recordHistory(bug, currentUser,
                    "assignee", oldAssigneeName, newAssignee.getUsername());
        }

        return convertToDTO(bug);
    }

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Deletes a bug and all its comments and history via CASCADE.
     * Admin and PM only.
     */
    @Transactional
    public void deleteBug(Long id) {
        if (!bugRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Bug not found with ID: " + id);
        }
        bugRepository.deleteById(id);
    }

    // =========================================================
    // DASHBOARD STATISTICS
    // =========================================================

    /**
     * Returns the count of bugs with a given status.
     * Used to populate the dashboard stat cards.
     */
    @Transactional(readOnly = true)
    public long countByStatus(BugStatus status) {
        return bugRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long getTotalBugCount() {
        return bugRepository.count();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /**
     * Loads a Bug entity by ID or throws ResourceNotFoundException.
     * Used by every method that needs to fetch a bug first.
     */
    private Bug findBugOrThrow(Long id) {
        return bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bug not found with ID: " + id));
    }

    /**
     * Creates and saves a BugHistory record.
     *
     * Called after every change so the full audit trail is maintained.
     * Every status change, reassignment, or field edit is recorded.
     *
     * @param bug          the bug that changed
     * @param changedBy    the user who made the change
     * @param field        name of the changed field
     * @param oldValue     value before the change (null for creation)
     * @param newValue     value after the change
     */
    private void recordHistory(Bug bug, User changedBy,
                               String field,
                               String oldValue, String newValue) {
        BugHistory entry = new BugHistory(
                bug, changedBy, field, oldValue, newValue);
        bugHistoryRepository.save(entry);
    }

    /**
     * Converts a Bug entity to a BugDTO for the web layer.
     *
     * Note: We access bug.getReporter() and bug.getAssignee() here.
     * These are LAZY-loaded @ManyToOne relationships. Because this
     * method runs inside a @Transactional method, the Hibernate
     * session is still open and the lazy load succeeds.
     *
     * If you call this outside a transaction, you get
     * LazyInitializationException — a common bug in Spring apps.
     */
    private BugDTO convertToDTO(Bug bug) {
        BugDTO dto = new BugDTO();
        dto.setId(bug.getId());
        dto.setTitle(bug.getTitle());
        dto.setDescription(bug.getDescription());
        dto.setSeverity(bug.getSeverity());
        dto.setPriority(bug.getPriority());
        dto.setStatus(bug.getStatus());
        dto.setProjectId(bug.getProject().getId());
        dto.setProjectName(bug.getProject().getName());
        dto.setCreatedAt(bug.getCreatedAt());
        dto.setUpdatedAt(bug.getUpdatedAt());

        // Reporter — may be null if user was deleted
        if (bug.getReporter() != null) {
            dto.setReporterName(bug.getReporter().getFullName());
            dto.setReporterId(bug.getReporter().getId());
        }

        // Assignee — may be null if unassigned
        if (bug.getAssignee() != null) {
            dto.setAssigneeName(bug.getAssignee().getFullName());
            dto.setAssigneeId(bug.getAssignee().getId());
        }

        // Comment count — size() on a LAZY list triggers a query here.
        // In Phase 7 we optimize this with a @Query that fetches counts
        // in the initial query to avoid N+1.
        dto.setCommentCount(
                (int) commentRepository.countByBug(bug));

        return dto;
    }
}