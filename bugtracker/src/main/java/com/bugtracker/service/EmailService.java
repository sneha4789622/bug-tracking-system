package com.bugtracker.service;

import com.bugtracker.model.Bug;
import com.bugtracker.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * EmailService — composes and sends email notifications.
 *
 * @Async on notification methods means they run in a separate
 * thread pool. The HTTP request does not wait for the email
 * to send before returning a response to the user.
 *
 * Why @Async for email?
 *   Sending email involves network I/O to an SMTP server.
 *   This can take 100ms–2 seconds. Making the user wait for
 *   this before seeing their page updated is bad UX.
 *   With @Async, the email is sent in the background while
 *   the user immediately sees the updated bug page.
 *
 * To enable @Async, we add @EnableAsync to a @Configuration class.
 * We add it to SecurityConfig since it is already a configuration.
 */
@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /**
     * @Value injects values from application.properties.
     * "${app.mail.from}" reads the app.mail.from property.
     * The default value after the colon is used if the
     * property is not defined.
     */
    @Value("${app.mail.from:noreply@bugtracker.com}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email to the developer when a bug is assigned to them.
     *
     * @Async — runs in a background thread.
     * The caller (BugService.assignBug) returns immediately.
     * The email sends asynchronously after the response is served.
     *
     * @param bug      the bug that was assigned
     * @param assignee the developer receiving the assignment
     */
    @Async
    public void sendBugAssignedNotification(Bug bug, User assignee) {

        if (!mailEnabled) {
            log.info("Email disabled. Would have sent assignment " +
                            "notification for Bug #{} to {}",
                    bug.getId(), assignee.getEmail());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            // Recipient
            message.setTo(assignee.getEmail());

            // Sender
            message.setFrom(fromAddress);

            // Subject
            message.setSubject(
                    "[Bug Tracker] Bug #" + bug.getId()
                            + " assigned to you: " + bug.getTitle());

            // Body — plain text
            message.setText(buildAssignmentEmailBody(bug, assignee));

            mailSender.send(message);

            log.info("Assignment notification sent to {} for Bug #{}",
                    assignee.getEmail(), bug.getId());

        } catch (MailException e) {
            // Log the failure but do NOT propagate it.
            // Email failure should never prevent the assignment
            // from being saved to the database.
            log.error("Failed to send assignment email for Bug #{}: {}",
                    bug.getId(), e.getMessage());
        }
    }

    /**
     * Sends an email when a bug's status changes.
     * Notifies the reporter that their bug has been updated.
     */
    @Async
    public void sendStatusChangeNotification(Bug bug,
                                             String oldStatus,
                                             String newStatus) {
        if (!mailEnabled) {
            log.info("Email disabled. Would have sent status change " +
                    "notification for Bug #{}", bug.getId());
            return;
        }

        // Only notify if the reporter exists and has an email
        if (bug.getReporter() == null) return;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(bug.getReporter().getEmail());
            message.setFrom(fromAddress);
            message.setSubject(
                    "[Bug Tracker] Bug #" + bug.getId()
                            + " status updated");
            message.setText(buildStatusChangeEmailBody(
                    bug, oldStatus, newStatus));

            mailSender.send(message);

        } catch (MailException e) {
            log.error("Failed to send status change email: {}",
                    e.getMessage());
        }
    }

    // ── Private: Email Body Builders ──────────────────────────

    private String buildAssignmentEmailBody(Bug bug, User assignee) {
        return """
                Hello %s,
                
                A bug has been assigned to you in the Bug Tracking System.
                
                Bug Details:
                ─────────────────────────────
                ID:          #%d
                Title:       %s
                Project:     %s
                Severity:    %s
                Priority:    %s
                Status:      %s
                
                Description:
                %s
                
                Please log in to review and begin working on this bug:
                http://localhost:8080/bugs/%d
                
                ─────────────────────────────
                This is an automated notification from Bug Tracker.
                """.formatted(
                assignee.getFullName(),
                bug.getId(),
                bug.getTitle(),
                bug.getProject().getName(),
                bug.getSeverity().getDisplayName(),
                bug.getPriority().getDisplayName(),
                bug.getStatus().getDisplayName(),
                bug.getDescription(),
                bug.getId()
        );
    }

    private String buildStatusChangeEmailBody(Bug bug,
                                              String oldStatus,
                                              String newStatus) {
        return """
                Hello %s,
                
                The status of your reported bug has been updated.
                
                Bug #%d: %s
                Status changed: %s → %s
                
                View the bug: http://localhost:8081/bugs/%d
                
                ─────────────────────────────
                This is an automated notification from Bug Tracker.
                """.formatted(
                bug.getReporter().getFullName(),
                bug.getId(),
                bug.getTitle(),
                oldStatus,
                newStatus,
                bug.getId()
        );
    }
}