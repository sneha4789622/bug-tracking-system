package com.bugtracker.service;
// Add these imports at the top of BugService.java
import com.bugtracker.repository.BugSpecification;
import com.bugtracker.dto.PageRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.bugtracker.dto.BugDTO;
import com.bugtracker.dto.BugFilterDTO;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.model.*;
import com.bugtracker.repository.*;
import com.bugtracker.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bugtracker.model.BugHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    /**
     * Logger for this class.
     * SLF4J is the API; Logback is the implementation Spring Boot wires in.
     * The class reference (BugService.class) appears in log output:
     *   2024-01-15 10:23:41 INFO  c.b.service.BugService - Bug #5 created
     */
    private static final Logger log =
            LoggerFactory.getLogger(BugService.class);


    private final BugRepository        bugRepository;
    private final ProjectRepository    projectRepository;
    private final UserRepository       userRepository;
    private final BugHistoryRepository bugHistoryRepository;
    private final CommentRepository    commentRepository;
    private final EmailService          emailService;

    // Updated constructor:
    public BugService(BugRepository        bugRepository,
                      ProjectRepository    projectRepository,
                      UserRepository       userRepository,
                      BugHistoryRepository bugHistoryRepository,
                      CommentRepository    commentRepository,
                      EmailService         emailService) {
        this.bugRepository        = bugRepository;
        this.projectRepository    = projectRepository;
        this.userRepository       = userRepository;
        this.bugHistoryRepository = bugHistoryRepository;
        this.commentRepository    = commentRepository;
        this.emailService         = emailService;
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
     * Returns a paginated, sorted, filtered page of bugs.
     *
     * This single method replaces both getAllBugs() and
     * getFilteredBugs() for the list page. It always paginates
     * and filters at the database level.
     *
     * @param filter      filter criteria (may have all-null fields)
     * @param pageRequest pagination and sorting parameters
     * @return a Page<BugDTO> containing results and navigation metadata
     */
    @Transactional(readOnly = true)
    public Page<BugDTO> getPagedBugs(BugFilterDTO filter,
                                     PageRequestDTO pageRequest) {

        // Build the Pageable object: page number, size, sort
        Pageable pageable = PageRequest.of(
                pageRequest.getZeroBasedPage(),
                pageRequest.getSafeSize(),
                Sort.by(pageRequest.getSortDirection(),
                        pageRequest.getSafeSortBy())
        );

        // Build the Specification from filter criteria
        BugSpecification spec = BugSpecification.from(filter);

        // Execute: one database query with WHERE + ORDER BY + LIMIT
        Page<Bug> bugPage = bugRepository.findAll(spec, pageable);

        // Convert each Bug entity to BugDTO, preserving page metadata
        return bugPage.map(this::convertToDTO);
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
    /**
     * Returns the full history for a bug, most recent first.
     * Used to display the audit trail on the bug detail page.
     */
    @Transactional(readOnly = true)
    public List<BugHistory> getBugHistory(Long bugId) {
        Bug bug = findBugOrThrow(bugId);
        return bugHistoryRepository.findByBugOrderByChangedAtDesc(bug);

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
    // ... add log statements to key methods ...

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

        log.info("Bug #{} created successfully", saved.getId());
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

        Bug bug          = findBugOrThrow(bugId);
        log.info("Bug #{} status change: {} → {} by '{}'",
                bugId,
                bug.getStatus(),
                newStatus,
                SecurityUtils.getCurrentUsername());
        User currentUser = SecurityUtils.getCurrentUser();
        BugStatus oldStatus = bug.getStatus();

        validateStatusTransition(oldStatus, newStatus);
        bug.setStatus(newStatus);
        recordHistory(bug, currentUser,
                "status", oldStatus.name(), newStatus.name());

        // Notify reporter of status change (async, non-blocking)
        emailService.sendStatusChangeNotification(
                bug, oldStatus.getDisplayName(), newStatus.getDisplayName());

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
        log.info("Bug #{} assigned to user ID {} by '{}'",
                bugId,
                assigneeId,
                SecurityUtils.getCurrentUsername());

        Bug bug          = findBugOrThrow(bugId);
        User currentUser = SecurityUtils.getCurrentUser();
        String oldAssigneeName = bug.getAssignee() != null
                ? bug.getAssignee().getUsername() : "Unassigned";

        if (assigneeId == null) {
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

            // Send async email notification to new assignee
            emailService.sendBugAssignedNotification(bug, newAssignee);
        }

        return convertToDTO(findBugOrThrow(bugId));
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
        log.warn("Bug #{} being deleted by '{}'",
                id,
                SecurityUtils.getCurrentUsername());
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