package com.pmei.company.repository;

import com.pmei.company.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for User data access.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email.
     *
     * Used during authentication.
     */
    Optional<User> findByEmail(String email);
}
