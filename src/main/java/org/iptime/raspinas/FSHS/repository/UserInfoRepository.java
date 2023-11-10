package org.iptime.raspinas.FSHS.repository;

import org.iptime.raspinas.FSHS.entity.user.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    boolean existsByUserEmail(String userEmail);
}
