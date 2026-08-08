package com.bugtracker.repository;

import com.bugtracker.dto.BugFilterDTO;
import com.bugtracker.model.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * BugSpecification — builds JPA Criteria API predicates from filter data.
 *
 * The Criteria API is a type-safe, programmatic way to build SQL WHERE
 * clauses without writing raw SQL strings.
 *
 * How it works:
 *   1. We implement Specification<Bug>
 *   2. The toPredicate() method receives three parameters:
 *      - root:    the FROM clause (Bug entity and its fields)
 *      - query:   the SELECT statement being built
 *      - builder: factory for creating predicates (=, LIKE, AND, OR)
 *   3. We build a list of predicates based on which filters are active
 *   4. We combine all predicates with AND
 *   5. Spring Data translates this into a SQL WHERE clause
 *
 * Example output for status=NEW, priority=HIGH:
 *   WHERE b.status = 'NEW' AND b.priority = 'HIGH'
 */
public class BugSpecification implements Specification<Bug> {

    /**
     * The filter criteria — comes from the URL query parameters.
     * We store it as a field because Specification.toPredicate()
     * does not accept extra parameters.
     */
    private final BugFilterDTO filter;

    public BugSpecification(BugFilterDTO filter) {
        this.filter = filter;
    }

    /**
     * Builds the WHERE clause predicates.
     *
     * @param root    represents the Bug entity in the FROM clause
     * @param query   the query being constructed
     * @param builder factory for building comparison expressions
     * @return a single combined predicate, or null if no filters active
     */
    @Override
    public Predicate toPredicate(Root<Bug> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder builder) {

        List<Predicate> predicates = new ArrayList<>();

        // ── Filter by Status ──────────────────────────────────
        // root.get("status") maps to the 'status' column in the bugs table
        // builder.equal creates: WHERE status = ?
        if (filter.getStatus() != null) {
            predicates.add(
                    builder.equal(root.get("status"), filter.getStatus())
            );
        }

        // ── Filter by Priority ────────────────────────────────
        if (filter.getPriority() != null) {
            predicates.add(
                    builder.equal(root.get("priority"), filter.getPriority())
            );
        }

        // ── Filter by Severity ────────────────────────────────
        if (filter.getSeverity() != null) {
            predicates.add(
                    builder.equal(root.get("severity"), filter.getSeverity())
            );
        }

        // ── Filter by Project ─────────────────────────────────
        // root.get("project") navigates the @ManyToOne relationship
        // root.get("project").get("id") maps to the project_id FK column
        if (filter.getProjectId() != null) {
            predicates.add(
                    builder.equal(
                            root.get("project").get("id"),
                            filter.getProjectId()
                    )
            );
        }

        // ── Filter by Assignee ────────────────────────────────
        if (filter.getAssigneeId() != null) {
            predicates.add(
                    builder.equal(
                            root.get("assignee").get("id"),
                            filter.getAssigneeId()
                    )
            );
        }

        // ── Keyword Search (title OR description) ─────────────
        // builder.like creates: WHERE field LIKE '%keyword%'
        // builder.lower ensures case-insensitive search on MySQL
        if (filter.getKeyword() != null
                && !filter.getKeyword().isBlank()) {

            String pattern = "%"
                    + filter.getKeyword().toLowerCase().trim()
                    + "%";

            // builder.or creates: WHERE (title LIKE ? OR description LIKE ?)
            predicates.add(
                    builder.or(
                            builder.like(
                                    builder.lower(root.get("title")),
                                    pattern
                            ),
                            builder.like(
                                    builder.lower(root.get("description")),
                                    pattern
                            )
                    )
            );
        }

        // ── Combine all predicates with AND ───────────────────
        if (predicates.isEmpty()) {
            // No filters — return null to select all records
            return null;
        }

        // builder.and creates: WHERE pred1 AND pred2 AND pred3
        return builder.and(predicates.toArray(new Predicate[0]));
    }

    /**
     * Static factory method — cleaner to call than new BugSpecification().
     * Usage: BugSpecification.from(filterDTO)
     */
    public static BugSpecification from(BugFilterDTO filter) {
        return new BugSpecification(filter);
    }
}