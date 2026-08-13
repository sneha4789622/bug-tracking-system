package com.bugtracker.service;

import com.bugtracker.dto.BugDTO;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.model.*;
import com.bugtracker.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BugServiceTest — unit tests for BugService.
 *
 * @ExtendWith(MockitoExtension.class) wires Mockito into JUnit 5.
 * It processes @Mock and @InjectMocks annotations automatically.
 *
 * No Spring context is loaded — tests run in milliseconds.
 *
 * Pattern for every test:
 *   GIVEN  — set up the test data and mock behaviour
 *   WHEN   — call the method under test
 *   THEN   — assert the expected result
 *
 * This pattern (also called Arrange-Act-Assert) makes every
 * test readable and its purpose immediately clear.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BugService Unit Tests")
class BugServiceTest {

    // ── Mocks: fake versions of all BugService dependencies ──

    @Mock
    private BugRepository bugRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BugHistoryRepository bugHistoryRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EmailService emailService;

    /**
     * @InjectMocks creates a REAL BugService instance and
     * injects all the @Mock fields into it via constructor injection.
     * This means BugService runs real code; only its
     * dependencies are fake.
     */
    @InjectMocks
    private BugService bugService;

    // ── Test fixtures (reusable test data) ───────────────────

    private User   testUser;
    private Project testProject;
    private Bug    testBug;

    @BeforeEach
    void setUp() {
        // Build a test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testdev");
        testUser.setFullName("Test Developer");
        testUser.setEmail("dev@test.com");
        testUser.setPassword("encoded_password");
        testUser.setEnabled(true);

        // Build a test project
        testProject = new Project();
        testProject.setId(10L);
        testProject.setName("Test Project");
        testProject.setStatus(ProjectStatus.ACTIVE);

        // Build a test bug
        testBug = new Bug();
        testBug.setId(100L);
        testBug.setTitle("Login button not working");
        testBug.setDescription("Clicking login shows a blank screen");
        testBug.setSeverity(BugSeverity.HIGH);
        testBug.setPriority(BugPriority.HIGH);
        testBug.setStatus(BugStatus.NEW);
        testBug.setProject(testProject);
        testBug.setReporter(testUser);

        /*
         * Set up the Spring Security context so SecurityUtils.getCurrentUser()
         * returns testUser inside the service methods.
         *
         * In a unit test there is no HTTP request or session, so we
         * manually populate the SecurityContextHolder — the same
         * place Spring Security stores the current user during a
         * real request.
         */
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        // Clear the security context after each test to prevent
        // one test's authentication from leaking into another
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // Tests for getBugById()
    // =========================================================

    @Test
    @DisplayName("getBugById returns BugDTO when bug exists")
    void getBugById_WhenBugExists_ReturnsBugDTO() {

        // GIVEN
        // when(mock.method(args)).thenReturn(value) tells Mockito:
        // "when bugRepository.findById(100L) is called, return
        //  Optional containing testBug"
        when(bugRepository.findById(100L))
                .thenReturn(Optional.of(testBug));

        when(commentRepository.countByBug(testBug))
                .thenReturn(3L);

        // WHEN
        BugDTO result = bugService.getBugById(100L);

        // THEN
        // AssertJ fluent assertions — much more readable than assertEquals
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTitle()).isEqualTo("Login button not working");
        assertThat(result.getStatus()).isEqualTo(BugStatus.NEW);
        assertThat(result.getSeverity()).isEqualTo(BugSeverity.HIGH);
        assertThat(result.getCommentCount()).isEqualTo(3);

        // Verify the repository was called exactly once with 100L
        // This ensures we're not loading the wrong record
        verify(bugRepository, times(1)).findById(100L);
    }

    @Test
    @DisplayName("getBugById throws ResourceNotFoundException when bug not found")
    void getBugById_WhenBugNotFound_ThrowsResourceNotFoundException() {

        // GIVEN
        // Optional.empty() simulates "bug not in database"
        when(bugRepository.findById(999L))
                .thenReturn(Optional.empty());

        // THEN
        // assertThatThrownBy captures the exception thrown by the lambda
        // and lets us assert on its type and message
        assertThatThrownBy(() -> bugService.getBugById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        // Verify findById was called — the method didn't short-circuit
        verify(bugRepository).findById(999L);
    }

    // =========================================================
    // Tests for createBug()
    // =========================================================

    @Test
    @DisplayName("createBug saves bug with NEW status regardless of DTO status")
    void createBug_AlwaysSetsStatusToNew() {

        // GIVEN
        BugDTO inputDTO = new BugDTO();
        inputDTO.setTitle("  Null pointer in login  ");  // note: leading/trailing spaces
        inputDTO.setDescription("NullPointerException at AuthService:42");
        inputDTO.setSeverity(BugSeverity.CRITICAL);
        inputDTO.setPriority(BugPriority.HIGH);
        inputDTO.setStatus(BugStatus.RESOLVED);   // user tries to set status
        inputDTO.setProjectId(10L);

        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(testProject));

        // ArgumentCaptor captures what was actually passed to save()
        // so we can assert on it
        ArgumentCaptor<Bug> bugCaptor =
                ArgumentCaptor.forClass(Bug.class);

        // Simulate save() returning the bug with an assigned ID
        when(bugRepository.save(bugCaptor.capture()))
                .thenAnswer(invocation -> {
                    Bug savedBug = invocation.getArgument(0);
                    savedBug.setId(101L);
                    return savedBug;
                });

        when(commentRepository.countByBug(any())).thenReturn(0L);

        // WHEN
        BugDTO result = bugService.createBug(inputDTO);

        // THEN
        Bug capturedBug = bugCaptor.getValue();

        // Business rule: status is ALWAYS NEW on creation
        assertThat(capturedBug.getStatus())
                .isEqualTo(BugStatus.NEW)
                .as("Status must always be NEW regardless of DTO");

        // Business rule: title should be trimmed
        assertThat(capturedBug.getTitle())
                .isEqualTo("Null pointer in login")
                .as("Title should be trimmed of whitespace");

        // Business rule: reporter = current logged-in user
        assertThat(capturedBug.getReporter())
                .isEqualTo(testUser)
                .as("Reporter must be the currently logged-in user");

        assertThat(result.getId()).isEqualTo(101L);

        // Verify history was recorded
        verify(bugHistoryRepository).save(any(BugHistory.class));
    }

    @Test
    @DisplayName("createBug throws ResourceNotFoundException when project not found")
    void createBug_WhenProjectNotFound_ThrowsException() {

        // GIVEN
        BugDTO dto = new BugDTO();
        dto.setTitle("Some bug");
        dto.setDescription("Some description for the bug");
        dto.setSeverity(BugSeverity.LOW);
        dto.setPriority(BugPriority.LOW);
        dto.setProjectId(999L);

        when(projectRepository.findById(999L))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> bugService.createBug(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");

        // Verify bug was never saved (project validation failed first)
        verify(bugRepository, never()).save(any());
    }

    // =========================================================
    // Tests for updateStatus()
    // =========================================================

    @Test
    @DisplayName("updateStatus: valid transition NEW → IN_PROGRESS succeeds")
    void updateStatus_ValidTransition_UpdatesStatus() {

        // GIVEN
        testBug.setStatus(BugStatus.NEW);

        when(bugRepository.findById(100L))
                .thenReturn(Optional.of(testBug));
        when(commentRepository.countByBug(any())).thenReturn(0L);

        // WHEN
        BugDTO result = bugService.updateStatus(100L, BugStatus.IN_PROGRESS);

        // THEN
        assertThat(result.getStatus()).isEqualTo(BugStatus.IN_PROGRESS);

        // Verify history entry was recorded for the status change
        ArgumentCaptor<BugHistory> historyCaptor =
                ArgumentCaptor.forClass(BugHistory.class);
        verify(bugHistoryRepository).save(historyCaptor.capture());

        BugHistory history = historyCaptor.getValue();
        assertThat(history.getFieldChanged()).isEqualTo("status");
        assertThat(history.getOldValue()).isEqualTo("NEW");
        assertThat(history.getNewValue()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("updateStatus: invalid transition NEW → RESOLVED throws exception for developer")
    void updateStatus_InvalidTransition_ThrowsIllegalStateException() {

        // GIVEN: developer user (not admin/PM) trying invalid transition
        testBug.setStatus(BugStatus.NEW);

        // Ensure current user is a developer (no admin/PM role)
        Role devRole = new Role("ROLE_DEVELOPER");
        testUser.addRole(devRole);

        when(bugRepository.findById(100L))
                .thenReturn(Optional.of(testBug));

        // WHEN / THEN
        assertThatThrownBy(
                () -> bugService.updateStatus(100L, BugStatus.RESOLVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");

        // Verify status was NOT changed (bug state should be unchanged)
        assertThat(testBug.getStatus()).isEqualTo(BugStatus.NEW);

        // Verify nothing was saved
        verify(bugHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: admin can make any transition")
    void updateStatus_AdminUser_BypassesTransitionRules() {

        // GIVEN: admin user
        Role adminRole = new Role("ROLE_ADMIN");
        testUser.addRole(adminRole);

        // Re-set the security context with the admin user
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        testBug.setStatus(BugStatus.NEW);

        when(bugRepository.findById(100L))
                .thenReturn(Optional.of(testBug));
        when(commentRepository.countByBug(any())).thenReturn(0L);

        // WHEN: admin jumps directly from NEW to RESOLVED
        BugDTO result = bugService.updateStatus(100L, BugStatus.RESOLVED);

        // THEN: allowed — admin bypasses transition rules
        assertThat(result.getStatus()).isEqualTo(BugStatus.RESOLVED);
    }

    // =========================================================
    // Tests for assignBug()
    // =========================================================

    @Test
    @DisplayName("assignBug sends email notification to new assignee")
    void assignBug_SendsEmailNotification() {

        // GIVEN
        User developer = new User();
        developer.setId(2L);
        developer.setUsername("devuser");
        developer.setFullName("Dev User");
        developer.setEmail("dev@company.com");

        when(bugRepository.findById(100L))
                .thenReturn(Optional.of(testBug));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(developer));
        when(commentRepository.countByBug(any())).thenReturn(0L);

        // WHEN
        bugService.assignBug(100L, 2L);

        // THEN: email service was called with correct arguments
        verify(emailService).sendBugAssignedNotification(testBug, developer);
        assertThat(testBug.getAssignee()).isEqualTo(developer);
    }

    @Test
    @DisplayName("assignBug with null assigneeId unassigns the bug")
    void assignBug_WithNullAssigneeId_UnassignsBug() {

        // GIVEN: bug currently has an assignee
        testBug.setAssignee(testUser);

        when(bugRepository.findById(100L))
                .thenReturn(Optional.of(testBug));
        when(commentRepository.countByBug(any())).thenReturn(0L);

        // WHEN: assign with null (unassign)
        bugService.assignBug(100L, null);

        // THEN
        assertThat(testBug.getAssignee()).isNull();

        // Email NOT sent when unassigning
        verify(emailService, never())
                .sendBugAssignedNotification(any(), any());
    }

    // =========================================================
    // Tests for deleteBug()
    // =========================================================

    @Test
    @DisplayName("deleteBug calls repository when bug exists")
    void deleteBug_WhenBugExists_DeletesSuccessfully() {

        // GIVEN
        when(bugRepository.existsById(100L)).thenReturn(true);

        // WHEN
        bugService.deleteBug(100L);

        // THEN: deleteById was called exactly once
        verify(bugRepository, times(1)).deleteById(100L);
    }

    @Test
    @DisplayName("deleteBug throws exception when bug does not exist")
    void deleteBug_WhenBugNotFound_ThrowsException() {

        // GIVEN
        when(bugRepository.existsById(999L)).thenReturn(false);

        // WHEN / THEN
        assertThatThrownBy(() -> bugService.deleteBug(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        // Verify deleteById was never called on a non-existent bug
        verify(bugRepository, never()).deleteById(any());
    }

    // =========================================================
    // Tests for countByStatus()
    // =========================================================

    @Test
    @DisplayName("countByStatus delegates to repository")
    void countByStatus_ReturnsDelegatedCount() {

        // GIVEN
        when(bugRepository.countByStatus(BugStatus.NEW))
                .thenReturn(7L);

        // WHEN
        long count = bugService.countByStatus(BugStatus.NEW);

        // THEN
        assertThat(count).isEqualTo(7L);
        verify(bugRepository).countByStatus(BugStatus.NEW);
    }
}