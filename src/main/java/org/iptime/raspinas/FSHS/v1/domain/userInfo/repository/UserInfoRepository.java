package org.iptime.raspinas.FSHS.v1.domain.userInfo.repository;

import org.iptime.raspinas.FSHS.v1.domain.userInfo.domain.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    boolean existsByUserEmail(String userEmail);
    Optional<UserInfo> findByUserEmail(String userEmail);
}
