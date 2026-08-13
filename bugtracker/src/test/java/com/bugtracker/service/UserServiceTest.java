package com.bugtracker.service;

import com.bugtracker.dto.RegistrationDTO;
import com.bugtracker.model.Role;
import com.bugtracker.model.User;
import com.bugtracker.repository.RoleRepository;
import com.bugtracker.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository  userRepository;
    @Mock private RoleRepository  roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private Role developerRole;

    @BeforeEach
    void setUp() {
        developerRole = new Role("ROLE_DEVELOPER");
        developerRole.setId(3L);
    }

    // =========================================================
    // Tests for loadUserByUsername()
    // =========================================================

    @Test
    @DisplayName("loadUserByUsername returns UserDetails when user found")
    void loadUserByUsername_WhenUserExists_ReturnsUserDetails() {

        // GIVEN
        User user = new User("johndoe", "john@test.com",
                "hashed_password", "John Doe");
        user.setId(1L);

        when(userRepository.findByUsername("johndoe"))
                .thenReturn(Optional.of(user));

        // WHEN
        var result = userService.loadUserByUsername("johndoe");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when user not found")
    void loadUserByUsername_WhenUserNotFound_ThrowsException() {

        // GIVEN
        when(userRepository.findByUsername("ghost"))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(
                () -> userService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    // =========================================================
    // Tests for registerUser()
    // =========================================================

    @Test
    @DisplayName("registerUser saves hashed password and assigns DEVELOPER role")
    void registerUser_ValidData_SavesUserWithHashedPassword() {

        // GIVEN
        RegistrationDTO dto = new RegistrationDTO();
        dto.setFullName("Alice Smith");
        dto.setUsername("alice");
        dto.setEmail("alice@test.com");
        dto.setPassword("securePass1!");
        dto.setConfirmPassword("securePass1!");

        when(userRepository.existsByUsername("alice"))
                .thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com"))
                .thenReturn(false);
        when(roleRepository.findByName("ROLE_DEVELOPER"))
                .thenReturn(Optional.of(developerRole));
        when(passwordEncoder.encode("securePass1!"))
                .thenReturn("$2a$10$hashed_value_here");

        // Capture what gets saved
        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(i -> i.getArgument(0));

        // WHEN
        userService.registerUser(dto);

        // THEN
        User savedUser = userCaptor.getValue();

        // Password must be hashed, never plain text
        assertThat(savedUser.getPassword())
                .isEqualTo("$2a$10$hashed_value_here")
                .doesNotContain("securePass1!")
                .as("Password must be stored as BCrypt hash");

        // Username should be lowercased
        assertThat(savedUser.getUsername())
                .isEqualTo("alice")
                .as("Username should be lowercase");

        // Default role should be DEVELOPER
        assertThat(savedUser.getRoles())
                .contains(developerRole)
                .as("New user must receive ROLE_DEVELOPER");

        assertThat(savedUser.isEnabled()).isTrue();

        // Verify password encoder was called
        verify(passwordEncoder).encode("securePass1!");
    }

    @Test
    @DisplayName("registerUser throws exception when username already taken")
    void registerUser_DuplicateUsername_ThrowsIllegalArgumentException() {

        // GIVEN
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("existinguser");
        dto.setEmail("new@test.com");
        dto.setPassword("password1!");
        dto.setConfirmPassword("password1!");
        dto.setFullName("Some User");

        when(userRepository.existsByUsername("existinguser"))
                .thenReturn(true);

        // WHEN / THEN
        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");

        // User must NOT be saved when username is taken
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser throws exception when email already registered")
    void registerUser_DuplicateEmail_ThrowsIllegalArgumentException() {

        // GIVEN
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("newuser");
        dto.setEmail("taken@test.com");
        dto.setPassword("password1!");
        dto.setConfirmPassword("password1!");
        dto.setFullName("New User");

        when(userRepository.existsByUsername("newuser"))
                .thenReturn(false);
        when(userRepository.existsByEmail("taken@test.com"))
                .thenReturn(true);

        // WHEN / THEN
        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser throws exception when passwords do not match")
    void registerUser_PasswordMismatch_ThrowsIllegalArgumentException() {

        // GIVEN
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("bob");
        dto.setEmail("bob@test.com");
        dto.setPassword("password1!");
        dto.setConfirmPassword("differentPassword!");
        dto.setFullName("Bob Jones");

        when(userRepository.existsByUsername("bob"))
                .thenReturn(false);
        when(userRepository.existsByEmail("bob@test.com"))
                .thenReturn(false);

        // WHEN / THEN
        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not match");

        verify(userRepository, never()).save(any());
    }
}