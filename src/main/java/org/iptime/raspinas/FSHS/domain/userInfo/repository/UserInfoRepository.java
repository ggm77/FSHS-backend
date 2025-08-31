package org.iptime.raspinas.FSHS.domain.userInfo.repository;

import org.iptime.raspinas.FSHS.domain.userInfo.domain.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    boolean existsByUserEmail(String userEmail);
    Optional<UserInfo> findByUserEmail(String userEmail);
}
