package com.seohamin.fshs.v2.domain.file;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.entity.Status;
import com.seohamin.fshs.v2.domain.file.dto.FileUpdateRequestDto;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.file.service.FileService;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.config.JpaAuditingConfig;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.StorageIoCore;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import com.seohamin.fshs.v2.global.init.SystemRootInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({JpaAuditingConfig.class, SystemRootInitializer.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FileServiceMoveTest {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @TempDir
    Path tempRoot;

    private FileService fileService;

    private static final String USERNAME = "tester";

    private Long archiveId;
    private Long reportId;

    @BeforeEach
    void setUp() throws IOException {
        // --- StorageManager 를 임시 디렉터리 루트로 수동 구성 ---
        final StorageManager storageManager = new StorageManager(new StorageIoCore());
        ReflectionTestUtils.setField(storageManager, "rootPath", tempRoot.toString());
        ReflectionTestUtils.setField(storageManager, "tempPath", tempRoot.resolve("tmp").toString());

        final Cache<Long, String> filePathCache = Caffeine.newBuilder().build();
        final Cache<String, Status> fileStatusCache = Caffeine.newBuilder().build();
        final Cache<String, Boolean> fileAccessCache = Caffeine.newBuilder().build();
        // 이동 경로에서 쓰지 않는 의존성(fileUploadProcessor, ffmpegProcessor, fileThumbnailProcessor)은 null
        fileService = new FileService(fileRepository, folderRepository, userRepository, storageManager,
                null, filePathCache, fileStatusCache, fileAccessCache, null, null);

        // --- 시스템 루트 ---
        if (folderRepository.findById(1L).isEmpty()) {
            folderRepository.insertSystemRoot("data", "data");
        }
        final Folder systemRoot = folderRepository.findById(1L).get();

        // --- DB 트리 구성: userA / {docs/{projects/{report.txt}}, archive} ---
        final Folder userARoot = persistFolder("userA", systemRoot, "userA");
        final Folder docs = persistFolder("docs", userARoot, "userA/docs");
        final Folder archive = persistFolder("archive", userARoot, "userA/archive");
        final Folder projects = persistFolder("projects", docs, "userA/docs/projects");
        final File report = persistFile("report.txt", projects, "userA/docs/projects/report.txt", "userA/docs/projects");

        // --- 유저: 루트 폴더 = userA ---
        final User user = userRepository.save(User.builder().username(USERNAME).password("pw").build());
        user.updateRootFolder(userARoot);

        archiveId = archive.getId();
        reportId = report.getId();

        // --- 디스크 트리 구성 ---
        mkdir("userA/docs/projects");
        mkdir("userA/archive");
        touch("userA/docs/projects/report.txt");

        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("파일 이동 : 파일을 다른 폴더로 옮기면 디스크와 DB 경로가 갱신된다")
    void moveFile_updatesDiskAndPaths() {
        // When : report.txt 를 archive 로 이동
        fileService.updateFile(reportId, new FileUpdateRequestDto(archiveId, null), USERNAME);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then : 디스크 이동
        assertThat(Files.exists(tempRoot.resolve("userA/archive/report.txt"))).isTrue();
        assertThat(Files.exists(tempRoot.resolve("userA/docs/projects/report.txt"))).isFalse();

        // Then : DB 경로 갱신
        final File report = fileRepository.findById(reportId).orElseThrow();
        assertThat(report.getRelativePath()).isEqualTo("userA/archive/report.txt");
        assertThat(report.getParentPath()).isEqualTo("userA/archive");
        assertThat(report.getParentFolder().getId()).isEqualTo(archiveId);
    }

    @Test
    @DisplayName("파일 이동 : DB 작업이 실패하면 디스크 이동이 일어나지 않는다")
    void moveFile_dbFailure_doesNotMoveDisk() {
        // Given : DB 상으로만 archive 아래 'report.txt' 가 이미 존재 (디스크엔 없음)
        //         → projects/report.txt 를 archive 로 옮기면 디스크 이동 전에
        //           flush 때 uk_file_path(parent, name) 충돌로 실패해야 한다
        final Folder archive = folderRepository.findById(archiveId).orElseThrow();
        persistFile("report.txt", archive, "userA/archive/report.txt", "userA/archive");
        testEntityManager.flush();
        testEntityManager.clear();

        // When & Then : 이동 시도 → 이름 충돌이 FILE_ALREADY_EXIST(400) 로 변환되어 전파
        assertThatThrownBy(() ->
                fileService.updateFile(reportId, new FileUpdateRequestDto(archiveId, null), USERNAME))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getExceptionCode())
                .isEqualTo(ExceptionCode.FILE_ALREADY_EXIST);

        // Then : 디스크는 손대지 않았으므로 원위치 그대로 (DB 먼저 → 실패 시 디스크 미이동)
        assertThat(Files.exists(tempRoot.resolve("userA/docs/projects/report.txt"))).isTrue();
        assertThat(Files.exists(tempRoot.resolve("userA/archive/report.txt"))).isFalse();
    }

    private Folder persistFolder(final String name, final Folder parent, final String relativePath) {
        final Folder folder = Folder.builder()
                .parentFolder(parent)
                .ownerId(null)
                .relativePath(relativePath)
                .name(name)
                .originUpdatedAt(Instant.now())
                .isRoot(false)
                .build();
        return folderRepository.save(folder);
    }

    private File persistFile(final String name, final Folder parent, final String relativePath, final String parentPath) {
        final int dot = name.lastIndexOf('.');
        final String base = dot > 0 ? name.substring(0, dot) : name;
        final String ext = dot > 0 ? name.substring(dot + 1) : "";
        final File file = File.builder()
                .parentFolder(parent)
                .ownerId(null)
                .uuid(UUID.randomUUID().toString())
                .name(name)
                .baseName(base)
                .extension(ext)
                .relativePath(relativePath)
                .parentPath(parentPath)
                .mimeType("text/plain")
                .size(1L)
                .originUpdatedAt(Instant.now())
                .category(Category.IMAGE)
                .build();
        return fileRepository.save(file);
    }

    private void mkdir(final String relativePath) throws IOException {
        Files.createDirectories(tempRoot.resolve(relativePath));
    }

    private void touch(final String relativePath) throws IOException {
        Files.writeString(tempRoot.resolve(relativePath), "x");
    }
}
