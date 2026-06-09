package com.bugtracker.repository;

import com.bugtracker.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RoleRepository
 *
 * JpaRepository<Role, Long> provides these methods for free:
 *   save(role)           — INSERT or UPDATE
 *   findById(id)         — SELECT by PK
 *   findAll()            — SELECT all
 *   delete(role)         — DELETE
 *   count()              — SELECT COUNT(*)
 *   existsById(id)       — SELECT EXISTS
 *
 * We add custom query methods by following Spring Data's
 * naming conventions. Spring generates the SQL automatically.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Spring Data parses "findByName" and generates:
     * SELECT * FROM roles WHERE name = ?
     *
     * Optional<Role> is returned rather than Role to force
     * callers to handle the case where the role doesn't exist,
     * avoiding NullPointerExceptions.
     */
    Optional<Role> findByName(String name);
}