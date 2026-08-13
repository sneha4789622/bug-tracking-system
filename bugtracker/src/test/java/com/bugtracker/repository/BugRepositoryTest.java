package com.bugtracker.repository;

import com.bugtracker.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * BugRepositoryTest — integration tests for BugRepository.
 *
 * @DataJpaTest loads ONLY the JPA layer:
 *   - Entity classes
 *   - Repository interfaces
 *   - JPA configuration
 *   - H2 in-memory database
 *
 * NOT loaded: controllers, services, security, email.
 * This makes tests fast while still testing real database operations.
 *
 * @Transactional is applied by @DataJpaTest — each test runs
 * in a transaction that is rolled back after the test.
 * This means database changes in one test do NOT affect others.
 * Each test always starts with a clean database state.
 *
 * TestEntityManager is a test-friendly wrapper around JPA
 * EntityManager. We use it to set up test data directly
 * without going through the service layer.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BugRepository Integration Tests")
class BugRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BugRepository bugRepository;

    // ── Test data ─────────────────────────────────────────────

    private User    savedUser;
    private Project savedProject;

    @BeforeEach
    void setUp() {
        // Create and persist a User
        User user = new User();
        user.setUsername("testdev");
        user.setEmail("testdev@test.com");
        user.setPassword("$2a$10$hashed");
        user.setFullName("Test Developer");

        // entityManager.persistAndFlush() saves to DB immediately
        // and clears the first-level cache so finds hit the DB
        savedUser = entityManager.persistAndFlush(user);

        // Create and persist a Project
        Project project = new Project();
        project.setName("Test Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(savedUser);

        savedProject = entityManager.persistAndFlush(project);
    }

    // ── Helper: creates and persists a Bug ───────────────────

    private Bug createBug(String title,
                          BugStatus status,
                          BugPriority priority) {
        Bug bug = new Bug();
        bug.setTitle(title);
        bug.setDescription("Description of: " + title);
        bug.setStatus(status);
        bug.setSeverity(BugSeverity.MEDIUM);
        bug.setPriority(priority);
        bug.setProject(savedProject);
        bug.setReporter(savedUser);
        return entityManager.persistAndFlush(bug);
    }

    // =========================================================
    // Tests for findByProject()
    // =========================================================

    @Test
    @DisplayName("findByProject returns all bugs for the project")
    void findByProject_ReturnsAllBugsForProject() {

        // GIVEN: three bugs in the project
        createBug("Bug A", BugStatus.NEW,         BugPriority.HIGH);
        createBug("Bug B", BugStatus.IN_PROGRESS, BugPriority.MEDIUM);
        createBug("Bug C", BugStatus.RESOLVED,    BugPriority.LOW);

        // Clear JPA cache so the query actually hits the DB
        entityManager.clear();

        // WHEN
        List<Bug> bugs = bugRepository.findByProject(savedProject);

        // THEN
        assertThat(bugs)
                .hasSize(3)
                .extracting(Bug::getTitle)
                .containsExactlyInAnyOrder("Bug A", "Bug B", "Bug C");
    }

    // =========================================================
    // Tests for findByStatus()
    // =========================================================

    @Test
    @DisplayName("findByStatus returns only bugs with matching status")
    void findByStatus_ReturnsOnlyMatchingBugs() {

        // GIVEN
        createBug("New Bug 1",      BugStatus.NEW,      BugPriority.HIGH);
        createBug("New Bug 2",      BugStatus.NEW,      BugPriority.LOW);
        createBug("Progress Bug",   BugStatus.IN_PROGRESS, BugPriority.HIGH);
        createBug("Resolved Bug",   BugStatus.RESOLVED, BugPriority.MEDIUM);

        entityManager.clear();

        // WHEN
        List<Bug> newBugs = bugRepository.findByStatus(BugStatus.NEW);

        // THEN
        assertThat(newBugs)
                .hasSize(2)
                .allMatch(b -> b.getStatus() == BugStatus.NEW)
                .as("Only NEW status bugs should be returned");
    }

    // =========================================================
    // Tests for countByStatus()
    // =========================================================

    @Test
    @DisplayName("countByStatus returns accurate count per status")
    void countByStatus_ReturnsCorrectCount() {

        // GIVEN
        createBug("Bug 1", BugStatus.NEW, BugPriority.HIGH);
        createBug("Bug 2", BugStatus.NEW, BugPriority.LOW);
        createBug("Bug 3", BugStatus.RESOLVED, BugPriority.MEDIUM);

        entityManager.clear();

        // WHEN
        long newCount      = bugRepository.countByStatus(BugStatus.NEW);
        long resolvedCount = bugRepository.countByStatus(BugStatus.RESOLVED);
        long closedCount   = bugRepository.countByStatus(BugStatus.CLOSED);

        // THEN
        assertThat(newCount).isEqualTo(2);
        assertThat(resolvedCount).isEqualTo(1);
        assertThat(closedCount).isZero();
    }

    // =========================================================
    // Tests for searchByKeyword()
    // =========================================================

    @Test
    @DisplayName("searchByKeyword finds bugs by title keyword")
    void searchByKeyword_FindsByTitle() {

        // GIVEN
        createBug("Login button broken", BugStatus.NEW, BugPriority.HIGH);
        createBug("Logout not working",  BugStatus.NEW, BugPriority.LOW);
        createBug("Dashboard crash",     BugStatus.NEW, BugPriority.MEDIUM);

        entityManager.clear();

        // WHEN: search for "login" (case-insensitive)
        List<Bug> results = bugRepository.searchByKeyword("login");

        // THEN: only the login bug should match
        assertThat(results)
                .hasSize(1)
                .first()
                .extracting(Bug::getTitle)
                .isEqualTo("Login button broken");
    }

    @Test
    @DisplayName("searchByKeyword is case-insensitive")
    void searchByKeyword_IsCaseInsensitive() {

        // GIVEN
        createBug("NULL pointer exception in auth",
                BugStatus.NEW, BugPriority.CRITICAL);

        entityManager.clear();

        // WHEN: search with uppercase — should still match
        List<Bug> results = bugRepository.searchByKeyword("NULL");

        // THEN
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("searchByKeyword finds bugs matching description")
    void searchByKeyword_FindsByDescription() {

        // GIVEN
        Bug bug = new Bug();
        bug.setTitle("Mysterious crash");
        bug.setDescription("StackOverflowError in recursive call");
        bug.setStatus(BugStatus.NEW);
        bug.setSeverity(BugSeverity.HIGH);
        bug.setPriority(BugPriority.HIGH);
        bug.setProject(savedProject);
        bug.setReporter(savedUser);
        entityManager.persistAndFlush(bug);
        entityManager.clear();

        // WHEN: keyword is in description, not title
        List<Bug> results =
                bugRepository.searchByKeyword("StackOverflow");

        // THEN
        assertThat(results)
                .hasSize(1)
                .first()
                .extracting(Bug::getTitle)
                .isEqualTo("Mysterious crash");
    }

    // =========================================================
    // Tests for findByAssignee()
    // =========================================================

    @Test
    @DisplayName("findByAssignee returns only bugs assigned to that user")
    void findByAssignee_ReturnsOnlyAssignedBugs() {

        // GIVEN: create a second user
        User secondUser = new User();
        secondUser.setUsername("otherdev");
        secondUser.setEmail("other@test.com");
        secondUser.setPassword("$2a$10$hashed2");
        secondUser.setFullName("Other Developer");
        User savedSecondUser = entityManager.persistAndFlush(secondUser);

        // Bug 1 assigned to testUser
        Bug bug1 = createBug("Bug for testUser",
                BugStatus.NEW, BugPriority.HIGH);
        bug1.setAssignee(savedUser);
        entityManager.persistAndFlush(bug1);

        // Bug 2 assigned to secondUser
        Bug bug2 = createBug("Bug for secondUser",
                BugStatus.NEW, BugPriority.LOW);
        bug2.setAssignee(savedSecondUser);
        entityManager.persistAndFlush(bug2);

        // Bug 3 unassigned
        createBug("Unassigned bug", BugStatus.NEW, BugPriority.MEDIUM);

        entityManager.clear();

        // WHEN
        List<Bug> bugsForTestUser =
                bugRepository.findByAssignee(savedUser);

        // THEN: only bug1 should be returned
        assertThat(bugsForTestUser)
                .hasSize(1)
                .first()
                .extracting(Bug::getTitle)
                .isEqualTo("Bug for testUser");
    }
}