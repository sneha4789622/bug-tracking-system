package com.bugtracker.controller;

import com.bugtracker.dto.ProjectDTO;
import com.bugtracker.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * ProjectController - Web Layer
 *
 * Handles all HTTP requests related to Projects.
 * Responsibilities:
 *   1. Receive HTTP requests
 *   2. Call the service layer (never access the database directly)
 *   3. Add results to the Model
 *   4. Return view names (templates to render)
 *
 * The controller knows NOTHING about how data is stored.
 * That is the service's concern.
 *
 * @Controller marks this as a Spring MVC controller.
 * @RequestMapping("/projects") sets the base URL for all methods.
 */
@Controller
@RequestMapping("/projects")
public class ProjectController {

    /**
     * We declare the dependency but do NOT create it with 'new'.
     * Spring injects it via the constructor (constructor injection).
     * 'final' means it cannot be reassigned after construction — good practice.
     */
    private final ProjectService projectService;

    /**
     * Constructor injection.
     * Spring sees this constructor, finds a ProjectService bean,
     * and injects it automatically.
     *
     * When there is only one constructor, @Autowired is optional.
     */
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // =========================================================
    // LIST ALL PROJECTS
    // GET /projects
    // =========================================================

    /**
     * Displays the list of all projects.
     *
     * @param model Spring's Model — a container for passing data to the view
     * @return the template name: templates/projects/list.html
     */
    @GetMapping
    public String listProjects(Model model) {
        // Ask the service for all projects
        List<ProjectDTO> projects = projectService.getAllProjects();

        // Add to model — "projects" is the key used in Thymeleaf as ${projects}
        model.addAttribute("projects", projects);
        model.addAttribute("pageTitle", "All Projects");

        return "projects/list";
    }

    // =========================================================
    // SHOW CREATE FORM
    // GET /projects/new
    // =========================================================

    /**
     * Displays the empty form for creating a new project.
     *
     * We add an empty ProjectDTO to the model because Thymeleaf's
     * th:object="${projectDTO}" requires an object to bind to.
     * Without it, the form cannot display or submit correctly.
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        // Provide an empty DTO for the form to bind to
        model.addAttribute("projectDTO", new ProjectDTO());
        model.addAttribute("pageTitle", "Create New Project");
        model.addAttribute("formAction", "/projects");  // POST destination

        return "projects/form";
    }

    // =========================================================
    // HANDLE CREATE FORM SUBMISSION
    // POST /projects
    // =========================================================

    /**
     * Processes the submitted create form.
     *
     * @Valid triggers validation of the ProjectDTO fields
     *        (@NotBlank, @Size annotations we defined in the DTO).
     *
     * BindingResult holds any validation errors.
     * CRITICAL: BindingResult must immediately follow the @Valid parameter.
     *           If you put Model before it, Spring won't capture errors.
     *
     * RedirectAttributes allows us to pass a "flash message" that
     * survives a redirect (displays once then disappears).
     */
    @PostMapping
    public String createProject(@Valid @ModelAttribute("projectDTO") ProjectDTO projectDTO,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        // If validation failed, redisplay the form with error messages
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Create New Project");
            model.addAttribute("formAction", "/projects");
            // Spring automatically re-populates projectDTO in the model
            return "projects/form";
        }

        // Validation passed — delegate to service
        projectService.createProject(projectDTO);

        // Add a success message that survives the redirect
        redirectAttributes.addFlashAttribute("successMessage",
                "Project '" + projectDTO.getName() + "' created successfully!");

        // Redirect to list — prevents duplicate form submission on refresh
        return "redirect:/projects";
    }

    // =========================================================
    // SHOW EDIT FORM
    // GET /projects/{id}/edit
    // =========================================================

    /**
     * Displays the edit form pre-populated with existing project data.
     *
     * @PathVariable extracts {id} from the URL path.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        // Fetch existing project — throws ResourceNotFoundException if not found
        // GlobalExceptionHandler will catch that and show the error page
        ProjectDTO projectDTO = projectService.getProjectById(id);

        model.addAttribute("projectDTO", projectDTO);
        model.addAttribute("pageTitle", "Edit Project");
        model.addAttribute("formAction", "/projects/" + id);

        return "projects/form";
    }

    // =========================================================
    // HANDLE EDIT FORM SUBMISSION
    // POST /projects/{id}
    // =========================================================

    /**
     * Processes the submitted edit form.
     * Note: HTML forms only support GET and POST.
     * We use POST for edit, then map it to our update method.
     * In Phase 7 we can add PUT via HiddenHttpMethodFilter.
     */
    @PostMapping("/{id}")
    public String updateProject(@PathVariable Long id,
                                @Valid @ModelAttribute("projectDTO") ProjectDTO projectDTO,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Project");
            model.addAttribute("formAction", "/projects/" + id);
            return "projects/form";
        }

        projectService.updateProject(id, projectDTO);

        redirectAttributes.addFlashAttribute("successMessage",
                "Project '" + projectDTO.getName() + "' updated successfully!");

        return "redirect:/projects";
    }

    // =========================================================
    // DELETE PROJECT
    // POST /projects/{id}/delete
    // =========================================================

    /**
     * Deletes a project.
     * We use POST (not GET) for deletion because GET requests
     * should never modify state. A browser or bot crawling links
     * could accidentally delete data via GET.
     */
    @PostMapping("/{id}/delete")
    public String deleteProject(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {

        // Fetch the name before deleting so we can show it in the success message
        ProjectDTO project = projectService.getProjectById(id);
        String projectName = project.getName();

        projectService.deleteProject(id);

        redirectAttributes.addFlashAttribute("successMessage",
                "Project '" + projectName + "' deleted successfully!");

        return "redirect:/projects";
    }

    // =========================================================
    // VIEW PROJECT DETAILS
    // GET /projects/{id}
    // =========================================================

    /**
     * Shows details of a single project.
     */
    @GetMapping("/{id}")
    public String viewProject(@PathVariable Long id, Model model) {
        ProjectDTO project = projectService.getProjectById(id);

        model.addAttribute("project", project);
        model.addAttribute("pageTitle", project.getName());

        return "projects/detail";
    }
}