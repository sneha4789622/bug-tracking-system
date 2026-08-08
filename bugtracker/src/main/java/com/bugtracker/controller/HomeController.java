package com.bugtracker.controller;

import com.bugtracker.model.BugStatus;
import com.bugtracker.model.User;
import com.bugtracker.service.BugService;
import com.bugtracker.service.ProjectService;
import com.bugtracker.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProjectService projectService;
    private final BugService     bugService;

    public HomeController(ProjectService projectService,
                          BugService     bugService) {
        this.projectService = projectService;
        this.bugService     = bugService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal User currentUser,
            Model model) {

        model.addAttribute("pageTitle",     "Dashboard");
        model.addAttribute("currentUser",   currentUser);

        // Real statistics from the database
        model.addAttribute("totalProjects", projectService.getProjectCount());
        model.addAttribute("totalBugs",     bugService.getTotalBugCount());
        model.addAttribute("openBugs",
                bugService.countByStatus(BugStatus.NEW)
                        + bugService.countByStatus(BugStatus.IN_PROGRESS));
        model.addAttribute("resolvedBugs",
                bugService.countByStatus(BugStatus.RESOLVED)
                        + bugService.countByStatus(BugStatus.CLOSED));

        // Developer's own assigned bugs
        model.addAttribute("myBugs",
                bugService.getMyAssignedBugs());

        return "dashboard";
    }

    @GetMapping("/health")
    @org.springframework.web.bind.annotation.ResponseBody
    public String health() {
        return "Application is running!";
    }
}