package org.iptime.raspinas.FSHS.repository;

import org.iptime.raspinas.FSHS.entity.user.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    boolean existsByUserEmail(String userEmail);
    Optional<UserInfo> findByUserEmail(String userEmail);
}
