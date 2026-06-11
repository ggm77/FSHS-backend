package com.seohamin.fshs.v2.global.init;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.user.entity.Role;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(2)
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 1) 유저가 1명이라도 존재하는지 확인
        if (userRepository.count() == 0) {
            log.info("[초기 어드민 계정 생성 중...]");

            // 2) 유저 엔티티 생성
            final User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .build();

            // 3) 관리자로 role 설정
            admin.updateRole(Role.ADMIN);

            // 4) 어드민 저장
            userRepository.save(admin);


            // 5) 시스템 루트 조회
            final Folder systemRootFolder = folderRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException("시스템 루트가 존재하지 않습니다."));

            // 6) 유저 정보에 루트 폴더 정보 저장
            admin.updateRootFolder(systemRootFolder);

            log.info("[초기 관리자 계정 생성 완료]");
        } else {
            log.info("[초기 관리자 계정 생성 스킵] 이미 사용자가 존재합니다.");
        }
    }
}
