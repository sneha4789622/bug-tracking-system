package com.bugtracker.controller;

import com.bugtracker.dto.BugDTO;
import com.bugtracker.dto.BugFilterDTO;
import com.bugtracker.dto.CommentDTO;
import com.bugtracker.model.*;
import com.bugtracker.service.BugService;
import com.bugtracker.service.CommentService;
import com.bugtracker.service.ProjectService;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * BugController — handles all HTTP requests for bugs and comments.
 *
 * Follows the same thin-controller pattern as ProjectController:
 *   - Receives requests
 *   - Delegates to services
 *   - Passes results to views
 *   - Never contains business logic
 */
@Controller
@RequestMapping("/bugs")
public class BugController {

    private final BugService     bugService;
    private final CommentService commentService;
    private final ProjectService projectService;
    private final UserService    userService;

    public BugController(BugService     bugService,
                         CommentService commentService,
                         ProjectService projectService,
                         UserService    userService) {
        this.bugService     = bugService;
        this.commentService = commentService;
        this.projectService = projectService;
        this.userService    = userService;
    }

    // =========================================================
    // LIST BUGS WITH FILTERS
    // GET /bugs
    // GET /bugs?status=NEW&priority=HIGH&keyword=login
    // =========================================================

    /**
     * Displays the bug list with optional filters.
     *
     * @ModelAttribute BugFilterDTO — Spring binds URL query parameters
     * to this object automatically.
     * e.g. /bugs?status=NEW binds to filter.status = BugStatus.NEW
     */
    @GetMapping
    public String listBugs(
            @ModelAttribute BugFilterDTO filter,
            @AuthenticationPrincipal User currentUser,
            Model model) {

        List<BugDTO> bugs = filter.hasActiveFilters()
                ? bugService.getFilteredBugs(filter)
                : bugService.getAllBugs();

        model.addAttribute("bugs",       bugs);
        model.addAttribute("filter",     filter);
        model.addAttribute("pageTitle",  "All Bugs");

        // Populate filter dropdowns
        model.addAttribute("allProjects",  projectService.getAllProjects());
        model.addAttribute("allStatuses",  BugStatus.values());
        model.addAttribute("allPriorities",BugPriority.values());
        model.addAttribute("allSeverities",BugSeverity.values());
        model.addAttribute("allDevelopers",userService.getAllDevelopers());

        return "bugs/list";
    }

    // =========================================================
    // SHOW CREATE FORM
    // GET /bugs/new
    // GET /bugs/new?projectId=3   (pre-select a project)
    // =========================================================

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(required = false) Long projectId,
            Model model) {

        BugDTO dto = new BugDTO();

        // Pre-select project if provided in URL
        if (projectId != null) {
            dto.setProjectId(projectId);
        }

        model.addAttribute("bugDTO",       dto);
        model.addAttribute("pageTitle",    "Report New Bug");
        model.addAttribute("allProjects",  projectService.getAllProjects());
        model.addAttribute("allSeverities",BugSeverity.values());
        model.addAttribute("allPriorities",BugPriority.values());
        model.addAttribute("allDevelopers",userService.getAllDevelopers());

        return "bugs/form";
    }

    // =========================================================
    // HANDLE CREATE FORM SUBMISSION
    // POST /bugs
    // =========================================================

    @PostMapping
    public String createBug(
            @Valid @ModelAttribute("bugDTO") BugDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle",    "Report New Bug");
            model.addAttribute("allProjects",  projectService.getAllProjects());
            model.addAttribute("allSeverities",BugSeverity.values());
            model.addAttribute("allPriorities",BugPriority.values());
            model.addAttribute("allDevelopers",userService.getAllDevelopers());
            return "bugs/form";
        }

        BugDTO created = bugService.createBug(dto);

        redirectAttributes.addFlashAttribute("successMessage",
                "Bug #" + created.getId() + " reported successfully!");

        return "redirect:/bugs/" + created.getId();
    }

    // =========================================================
    // VIEW BUG DETAIL
    // GET /bugs/{id}
    // =========================================================

    /**
     * Displays full bug detail with comment thread and history.
     *
     * We also pass an empty CommentDTO so the add-comment form
     * has a model object to bind to.
     */
    @GetMapping("/{id}")
    public String viewBug(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            Model model) {

        BugDTO bug = bugService.getBugById(id);

        model.addAttribute("bug",         bug);
        model.addAttribute("pageTitle",   "Bug #" + id);
        model.addAttribute("comments",    commentService.getCommentsForBug(id));
        model.addAttribute("commentDTO",  new CommentDTO());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("allStatuses",  BugStatus.values());
        model.addAttribute("allDevelopers",userService.getAllDevelopers());

        // Add history for the audit trail panel
        model.addAttribute("history",     bugService.getBugHistory(id));

        return "bugs/detail";
    }

    // =========================================================
    // SHOW EDIT FORM
    // GET /bugs/{id}/edit
    // =========================================================

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        BugDTO bug = bugService.getBugById(id);

        model.addAttribute("bugDTO",       bug);
        model.addAttribute("pageTitle",    "Edit Bug #" + id);
        model.addAttribute("allProjects",  projectService.getAllProjects());
        model.addAttribute("allSeverities",BugSeverity.values());
        model.addAttribute("allPriorities",BugPriority.values());
        model.addAttribute("allDevelopers",userService.getAllDevelopers());

        return "bugs/form";
    }

    // =========================================================
    // HANDLE EDIT FORM SUBMISSION
    // POST /bugs/{id}
    // =========================================================

    @PostMapping("/{id}")
    public String updateBug(
            @PathVariable Long id,
            @Valid @ModelAttribute("bugDTO") BugDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle",    "Edit Bug #" + id);
            model.addAttribute("allProjects",  projectService.getAllProjects());
            model.addAttribute("allSeverities",BugSeverity.values());
            model.addAttribute("allPriorities",BugPriority.values());
            model.addAttribute("allDevelopers",userService.getAllDevelopers());
            return "bugs/form";
        }

        bugService.updateBug(id, dto);

        redirectAttributes.addFlashAttribute("successMessage",
                "Bug #" + id + " updated successfully!");

        return "redirect:/bugs/" + id;
    }

    // =========================================================
    // UPDATE STATUS
    // POST /bugs/{id}/status
    // =========================================================

    /**
     * Updates only the status field.
     * @RequestParam reads a single form field or URL parameter.
     */
    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam BugStatus newStatus,
            RedirectAttributes redirectAttributes) {

        try {
            bugService.updateStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bug status updated to: " + newStatus.getDisplayName());
        } catch (IllegalStateException e) {
            // Invalid transition — show error but stay on the bug page
            redirectAttributes.addFlashAttribute("errorMessage",
                    e.getMessage());
        }

        return "redirect:/bugs/" + id;
    }

    // =========================================================
    // ASSIGN BUG
    // POST /bugs/{id}/assign
    // =========================================================

    @PostMapping("/{id}/assign")
    public String assignBug(
            @PathVariable Long id,
            @RequestParam(required = false) Long assigneeId,
            RedirectAttributes redirectAttributes) {

        bugService.assignBug(id, assigneeId);

        redirectAttributes.addFlashAttribute("successMessage",
                assigneeId != null
                        ? "Bug assigned successfully!"
                        : "Bug unassigned.");

        return "redirect:/bugs/" + id;
    }

    // =========================================================
    // DELETE BUG
    // POST /bugs/{id}/delete
    // =========================================================

    @PostMapping("/{id}/delete")
    public String deleteBug(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        bugService.deleteBug(id);

        redirectAttributes.addFlashAttribute("successMessage",
                "Bug #" + id + " deleted.");

        return "redirect:/bugs";
    }

    // =========================================================
    // ADD COMMENT
    // POST /bugs/{id}/comments
    // =========================================================

    @PostMapping("/{id}/comments")
    public String addComment(
            @PathVariable Long id,
            @Valid @ModelAttribute("commentDTO") CommentDTO dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // Flash the error and redirect back — the form is on the detail page
            redirectAttributes.addFlashAttribute("commentError",
                    "Comment cannot be empty.");
            return "redirect:/bugs/" + id;
        }

        commentService.addComment(id, dto);

        redirectAttributes.addFlashAttribute("successMessage",
                "Comment added.");

        // Redirect to the bottom of the page where comments are shown
        return "redirect:/bugs/" + id + "#comments";
    }

    // =========================================================
    // DELETE COMMENT
    // POST /bugs/{bugId}/comments/{commentId}/delete
    // =========================================================

    @PostMapping("/{bugId}/comments/{commentId}/delete")
    public String deleteComment(
            @PathVariable Long bugId,
            @PathVariable Long commentId,
            RedirectAttributes redirectAttributes) {

        try {
            commentService.deleteComment(commentId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Comment deleted.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    e.getMessage());
        }

        return "redirect:/bugs/" + bugId + "#comments";
    }
}