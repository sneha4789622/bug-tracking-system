package com.bugtracker.repository;

import com.bugtracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // SELECT EXISTS (SELECT 1 FROM users WHERE username = ?)
    boolean existsByUsername(String username);

    // SELECT EXISTS (SELECT 1 FROM users WHERE email = ?)
    boolean existsByEmail(String email);
}