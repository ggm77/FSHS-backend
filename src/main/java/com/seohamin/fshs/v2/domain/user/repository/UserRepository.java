package com.seohamin.fshs.v2.domain.user.repository;

import com.seohamin.fshs.v2.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(final String username);
    Optional<User> findByUsername(final String username);
}
