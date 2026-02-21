package com.seohamin.fshs.v2.global.init;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Order(1)
@Slf4j
public class SystemRootInitializer implements CommandLineRunner {

    public static final String SYSTEM_ROOT_NAME = "</SYSTEM/ROOT/>";

    private final FolderRepository folderRepository;

    @Override
    @Transactional
    public void run(String... args) {

        log.info("[시스템 루트 폴더 확인 중...]");

        // 1) 1번 폴더 조회
        final Optional<Folder> systemRoot = folderRepository.findById(1L);

        // 2) 1번 폴더가 존재하는지 확인
        // 존재할 때
        if (systemRoot.isPresent()) {
            if (systemRoot.get().getName().equals(SYSTEM_ROOT_NAME)) {
                log.info("[시스템 루트 확인 됨]");
            }
            else{
                log.error("[잘못된 시스템 루트 폴더입니다. 서버를 종료합니다.]");
                throw new IllegalStateException("올바르지 않은 시스템 루트 폴더");
            }
        }
        // 존재하지 않을 때
        else {
            log.info("[시스템 루트 폴더가 없습니다. 생성을 시작합니다.");

            // 3) 1번 폴더 없으면 시스템 루트 폴더 생성
            try {
                folderRepository.insertSystemRoot();
                log.info("[시스템 루트 폴더가 생성되었습니다.]");
            } catch (Exception ex) {
                log.error("[시스템 루트 폴더 생성 중 오류 발생] - {}", ex.getMessage());
                throw new IllegalStateException("시스템 루트 폴더 생성 중 오류 발생", ex);
            }
        }
    }
}
