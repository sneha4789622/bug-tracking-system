package com.bugtracker.controller;
// Add these imports
import com.bugtracker.dto.PageRequestDTO;
import org.springframework.data.domain.Page;
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
import com.bugtracker.service.AttachmentService;
import com.bugtracker.model.Attachment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

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
    private final AttachmentService attachmentService;


    public BugController(BugService     bugService,
                         CommentService commentService,
                         ProjectService projectService,
                         UserService    userService,
                         AttachmentService attachmentService) {
        this.bugService     = bugService;
        this.commentService = commentService;
        this.projectService = projectService;
        this.userService    = userService;
        this.attachmentService = attachmentService;

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
            @ModelAttribute PageRequestDTO pageRequest,
            @AuthenticationPrincipal User currentUser,
            Model model) {

        // Single database call — filtered, sorted, paginated
        Page<BugDTO> bugPage = bugService.getPagedBugs(filter, pageRequest);

        model.addAttribute("bugPage",      bugPage);
        model.addAttribute("bugs",         bugPage.getContent());
        model.addAttribute("filter",       filter);
        model.addAttribute("pageRequest",  pageRequest);
        model.addAttribute("pageTitle",    "All Bugs");

        // Total pages for pagination controls
        model.addAttribute("totalPages",   bugPage.getTotalPages());
        model.addAttribute("currentPage",  pageRequest.getPage());
        model.addAttribute("totalItems",   bugPage.getTotalElements());
        // Page number range for buttons (max 5 buttons shown)
        int pageStart = Math.max(1, pageRequest.getPage() - 2);
        int pageEnd   = Math.min(bugPage.getTotalPages(),
                pageRequest.getPage() + 2);

        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd",   pageEnd);

        // Filter dropdowns
        model.addAttribute("allProjects",   projectService.getAllProjects());
        model.addAttribute("allStatuses",   BugStatus.values());
        model.addAttribute("allPriorities", BugPriority.values());
        model.addAttribute("allSeverities", BugSeverity.values());
        model.addAttribute("allDevelopers", userService.getAllDevelopers());

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
        model.addAttribute("attachments", attachmentService.getAttachmentsForBug(id));

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
    // ─── UPLOAD ATTACHMENT ────────────────────────────────────────
// POST /bugs/{id}/attachments

    @PostMapping("/{id}/attachments")
    public String uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {
            attachmentService.uploadAttachment(id, file);
            redirectAttributes.addFlashAttribute("successMessage",
                    "File '" + file.getOriginalFilename()
                            + "' uploaded successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Upload failed: " + e.getMessage());
        }

        return "redirect:/bugs/" + id + "#attachments";
    }

// ─── DOWNLOAD ATTACHMENT ──────────────────────────────────────
// GET /bugs/{bugId}/attachments/{attachmentId}/download

    @GetMapping("/{bugId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long bugId,
            @PathVariable Long attachmentId) {

        try {
            Attachment attachment =
                    attachmentService.getAttachment(attachmentId);
            Resource resource =
                    attachmentService.loadAttachment(attachmentId);

            return ResponseEntity.ok()
                    // Content-Disposition: attachment prompts browser to
                    // download rather than display inline
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\""
                                    + attachment.getOriginalFilename() + "\"")
                    .contentType(MediaType.parseMediaType(
                            attachment.getContentType()))
                    .contentLength(attachment.getFileSize())
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

// ─── DELETE ATTACHMENT ────────────────────────────────────────
// POST /bugs/{bugId}/attachments/{attachmentId}/delete

    @PostMapping("/{bugId}/attachments/{attachmentId}/delete")
    public String deleteAttachment(
            @PathVariable Long bugId,
            @PathVariable Long attachmentId,
            RedirectAttributes redirectAttributes) {

        try {
            attachmentService.deleteAttachment(attachmentId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Attachment deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Delete failed: " + e.getMessage());
        }

        return "redirect:/bugs/" + bugId + "#attachments";
    }
}