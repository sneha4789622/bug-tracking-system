package com.bugtracker.repository;

import com.bugtracker.model.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setPassword("$2a$10$hashed_password");
        user.setFullName("Alice Smith");
        savedUser = entityManager.persistAndFlush(user);
    }

    @Test
    @DisplayName("findByUsername returns user when username exists")
    void findByUsername_WhenExists_ReturnsUser() {

        Optional<User> result = userRepository.findByUsername("alice");

        assertThat(result)
                .isPresent()
                .get()
                .extracting(User::getEmail)
                .isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("findByUsername returns empty when username not found")
    void findByUsername_WhenNotFound_ReturnsEmpty() {

        Optional<User> result =
                userRepository.findByUsername("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByUsername returns true for existing username")
    void existsByUsername_WhenExists_ReturnsTrue() {

        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isFalse();
    }

    @Test
    @DisplayName("existsByEmail returns true for existing email")
    void existsByEmail_WhenExists_ReturnsTrue() {

        assertThat(userRepository.existsByEmail("alice@test.com"))
                .isTrue();
        assertThat(userRepository.existsByEmail("other@test.com"))
                .isFalse();
    }

    @Test
    @DisplayName("username and email must be unique")
    void save_DuplicateUsername_ThrowsException() {

        User duplicate = new User();
        duplicate.setUsername("alice");        // same username
        duplicate.setEmail("different@test.com");
        duplicate.setPassword("$2a$10$other");
        duplicate.setFullName("Alice Clone");

        // Persisting a duplicate unique field throws
        // in H2 just like in MySQL
        assertThatThrownBy(
                () -> entityManager.persistAndFlush(duplicate))
                .isInstanceOf(Exception.class)
                .as("Duplicate username should be rejected by DB constraint");
    }
}