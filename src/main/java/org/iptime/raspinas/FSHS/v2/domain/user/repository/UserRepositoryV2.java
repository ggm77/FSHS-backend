package org.iptime.raspinas.FSHS.v2.domain.user.repository;

import org.iptime.raspinas.FSHS.v2.domain.user.entity.UserV2;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepositoryV2 extends JpaRepository<UserV2, Long> {
    boolean existsByUsername(final String username);
    Optional<UserV2> findByUsername(final String username);
}
