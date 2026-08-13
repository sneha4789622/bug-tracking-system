package com.bugtracker.controller;

import com.bugtracker.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // =========================================================
    // GET /auth/login
    // These are PUBLIC endpoints — use @WithMockUser to bypass
    // the 401 that Spring Security applies in @WebMvcTest
    // =========================================================

    @Test
    @DisplayName("GET /auth/login returns 200 with login template")
    @WithMockUser  // ← ADD THIS to all public endpoint tests
    void showLoginPage_Returns200() throws Exception {

        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /auth/login?error shows error message")
    @WithMockUser
    void showLoginPage_WithErrorParam_AddsErrorMessage()
            throws Exception {

        mockMvc.perform(get("/auth/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /auth/login?logout shows logout success message")
    @WithMockUser
    void showLoginPage_WithLogoutParam_AddsSuccessMessage()
            throws Exception {

        mockMvc.perform(get("/auth/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("successMessage"));
    }

    // =========================================================
    // GET /auth/register
    // =========================================================

    @Test
    @DisplayName("GET /auth/register returns 200 with registration template")
    @WithMockUser
    void showRegistrationForm_Returns200() throws Exception {

        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registrationDTO"));
    }

    // =========================================================
    // POST /auth/register
    // =========================================================

    @Test
    @DisplayName("POST /auth/register with valid data redirects to login")
    @WithMockUser
    void processRegistration_ValidData_RedirectsToLogin()
            throws Exception {

        doNothing().when(userService).registerUser(any());

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName",        "Jane Doe")
                        .param("username",        "janedoe")
                        .param("email",           "jane@test.com")
                        .param("password",        "SecurePass1!")
                        .param("confirmPassword", "SecurePass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /auth/register with blank username stays on form")
    @WithMockUser
    void processRegistration_BlankUsername_ReturnsFormWithErrors()
            throws Exception {

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName",        "Jane Doe")
                        .param("username",        "")
                        .param("email",           "jane@test.com")
                        .param("password",        "SecurePass1!")
                        .param("confirmPassword", "SecurePass1!"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());

        verify(userService, never()).registerUser(any());
    }

    @Test
    @DisplayName("POST /auth/register with business error shows error")
    @WithMockUser
    void processRegistration_UsernameAlreadyTaken_ShowsError()
            throws Exception {

        doThrow(new IllegalArgumentException(
                "Username 'janedoe' is already taken"))
                .when(userService).registerUser(any());

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName",        "Jane Doe")
                        .param("username",        "janedoe")
                        .param("email",           "jane@test.com")
                        .param("password",        "SecurePass1!")
                        .param("confirmPassword", "SecurePass1!"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMessage"));
    }
}