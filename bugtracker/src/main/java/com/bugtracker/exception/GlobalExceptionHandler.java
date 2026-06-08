package com.bugtracker.exception;


import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * GlobalExceptionHandler
 *
 * @ControllerAdvice makes this class apply to ALL controllers
 * in the application. Any exception thrown from any controller,
 * service, or repository that is not caught locally will be
 * intercepted here.
 *
 * This is the professional alternative to try/catch blocks
 * scattered across every controller method.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException anywhere in the application.
     *
     * @ExceptionHandler(ResourceNotFoundException.class) tells Spring:
     * "When a ResourceNotFoundException occurs, call this method."
     *
     * @param exception the thrown exception (Spring injects it automatically)
     * @param model     the Model to pass data to the error view
     * @return the name of the error template to render
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException exception,
                                         Model model) {
        // Pass the error message to the view
        model.addAttribute("errorTitle", "Resource Not Found");
        model.addAttribute("errorMessage", exception.getMessage());
        model.addAttribute("errorCode", "404");

        // Render templates/error/error.html
        return "error/error";
    }

    /**
     * Catches any other unexpected exception.
     * This is the safety net — no unhandled exception should
     * ever show a raw stack trace to the user.
     *
     * @param exception any Exception not handled by a more specific handler
     * @param model     the Model for the error view
     * @return the error template
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception exception,
                                         Model model) {
        model.addAttribute("errorTitle", "Something Went Wrong");
        model.addAttribute("errorMessage",
                "An unexpected error occurred. Please try again.");
        model.addAttribute("errorCode", "500");

        // Log the actual exception for debugging (we'll add proper logging later)
        System.err.println("Unhandled exception: " + exception.getMessage());
        exception.printStackTrace();

        return "error/error";
    }
}