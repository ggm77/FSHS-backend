package com.seohamin.fshs.v2.domain.folder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.dto.FolderRequestDto;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.folder.service.FolderService;
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
class FolderServiceMoveTest {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @TempDir
    Path tempRoot;

    private FolderService folderService;

    private static final String USERNAME = "tester";

    private Long docsId;
    private Long archiveId;
    private Long projectsId;
    private Long subId;
    private Long reportId;
    private Long nestedFileId;

    @BeforeEach
    void setUp() throws IOException {
        // --- StorageManager 를 임시 디렉터리 루트로 수동 구성 ---
        final StorageManager storageManager = new StorageManager(new StorageIoCore());
        ReflectionTestUtils.setField(storageManager, "rootPath", tempRoot.toString());
        ReflectionTestUtils.setField(storageManager, "tempPath", tempRoot.resolve("tmp").toString());

        final Cache<Long, String> filePathCache = Caffeine.newBuilder().build();
        final Cache<String, Boolean> fileAccessCache = Caffeine.newBuilder().build();
        folderService = new FolderService(folderRepository, userRepository, storageManager, filePathCache, fileAccessCache);

        // --- 시스템 루트 ---
        if (folderRepository.findById(1L).isEmpty()) {
            folderRepository.insertSystemRoot("data", "data");
        }
        final Folder systemRoot = folderRepository.findById(1L).get();

        // --- DB 트리 구성: userA / {docs/{projects/{report.txt, sub/{nested.txt}}}, archive} ---
        final Folder userARoot = persistFolder("userA", systemRoot, "userA");
        final Folder docs = persistFolder("docs", userARoot, "userA/docs");
        final Folder archive = persistFolder("archive", userARoot, "userA/archive");
        final Folder projects = persistFolder("projects", docs, "userA/docs/projects");
        final Folder sub = persistFolder("sub", projects, "userA/docs/projects/sub");
        final File report = persistFile("report.txt", projects, "userA/docs/projects/report.txt", "userA/docs/projects");
        final File nested = persistFile("nested.txt", sub, "userA/docs/projects/sub/nested.txt", "userA/docs/projects/sub");

        // --- 유저: 루트 폴더 = userA ---
        final User user = userRepository.save(User.builder().username(USERNAME).password("pw").build());
        user.updateRootFolder(userARoot);

        docsId = docs.getId();
        archiveId = archive.getId();
        projectsId = projects.getId();
        subId = sub.getId();
        reportId = report.getId();
        nestedFileId = nested.getId();

        // --- 디스크 트리 구성 ---
        mkdir("userA/docs/projects/sub");
        mkdir("userA/archive");
        touch("userA/docs/projects/report.txt");
        touch("userA/docs/projects/sub/nested.txt");

        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("폴더 이동 : 폴더를 다른 폴더로 옮기면 디스크와 DB의 하위 경로가 모두 갱신된다")
    void moveFolder_updatesDiskAndSubtreePaths() {
        // When : projects 를 archive 아래로 이동
        folderService.updateFolder(projectsId, new FolderRequestDto(archiveId, null), USERNAME);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then : 디스크가 실제로 이동됨
        assertThat(Files.exists(tempRoot.resolve("userA/archive/projects/report.txt"))).isTrue();
        assertThat(Files.exists(tempRoot.resolve("userA/archive/projects/sub/nested.txt"))).isTrue();
        assertThat(Files.exists(tempRoot.resolve("userA/docs/projects"))).isFalse();

        // Then : DB 의 폴더 자신 + 하위 폴더/파일 경로가 전부 갱신됨 (삭제되지 않음)
        final Folder projects = folderRepository.findById(projectsId).orElseThrow();
        assertThat(projects.getRelativePath()).isEqualTo("userA/archive/projects");
        assertThat(projects.getParentFolder().getId()).isEqualTo(archiveId);

        final Folder sub = folderRepository.findById(subId).orElseThrow();
        assertThat(sub.getRelativePath()).isEqualTo("userA/archive/projects/sub");

        final File report = fileRepository.findById(reportId).orElseThrow();
        assertThat(report.getRelativePath()).isEqualTo("userA/archive/projects/report.txt");
        assertThat(report.getParentPath()).isEqualTo("userA/archive/projects");

        final File nested = fileRepository.findById(nestedFileId).orElseThrow();
        assertThat(nested.getRelativePath()).isEqualTo("userA/archive/projects/sub/nested.txt");
        assertThat(nested.getParentPath()).isEqualTo("userA/archive/projects/sub");
    }

    @Test
    @DisplayName("폴더 이름 변경 : 이름을 바꾸면 디스크와 DB의 하위 경로가 모두 갱신된다")
    void renameFolder_updatesDiskAndSubtreePaths() {
        // When : docs -> documents 로 이름 변경
        folderService.updateFolder(docsId, new FolderRequestDto(null, "documents"), USERNAME);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then : 디스크 이름 변경
        assertThat(Files.exists(tempRoot.resolve("userA/documents/projects/sub/nested.txt"))).isTrue();
        assertThat(Files.exists(tempRoot.resolve("userA/docs"))).isFalse();

        // Then : DB 갱신
        final Folder docs = folderRepository.findById(docsId).orElseThrow();
        assertThat(docs.getName()).isEqualTo("documents");
        assertThat(docs.getRelativePath()).isEqualTo("userA/documents");

        final Folder projects = folderRepository.findById(projectsId).orElseThrow();
        assertThat(projects.getRelativePath()).isEqualTo("userA/documents/projects");

        final File nested = fileRepository.findById(nestedFileId).orElseThrow();
        assertThat(nested.getRelativePath()).isEqualTo("userA/documents/projects/sub/nested.txt");
        assertThat(nested.getParentPath()).isEqualTo("userA/documents/projects/sub");
    }

    @Test
    @DisplayName("폴더 이동 : 자기 자신의 하위 폴더로는 옮길 수 없다")
    void moveFolder_intoOwnDescendant_throws() {
        // When & Then : docs 를 docs 의 하위인 projects 로 이동 시도
        assertThatThrownBy(() ->
                folderService.updateFolder(docsId, new FolderRequestDto(projectsId, null), USERNAME))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getExceptionCode())
                .isEqualTo(ExceptionCode.RESTRICT_MOVE_INTO_DESCENDANT);

        // 디스크는 그대로 유지됨
        assertThat(Files.exists(tempRoot.resolve("userA/docs/projects"))).isTrue();
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
