package com.seohamin.fshs.v2.domain.folder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.folder.service.FolderService;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.config.JpaAuditingConfig;
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
class FolderServiceDeleteTest {

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

    private Long userARootId;
    private Long docsId;
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
        folderService = new FolderService(folderRepository, fileRepository, userRepository, storageManager, filePathCache, fileAccessCache);

        // --- 시스템 루트 ---
        if (folderRepository.findById(1L).isEmpty()) {
            folderRepository.insertSystemRoot("data", "data");
        }
        final Folder systemRoot = folderRepository.findById(1L).get();

        // --- DB 트리 구성: userA / {docs/{projects/{report.txt, sub/{nested.txt}}}} ---
        final Folder userARoot = persistFolder("userA", systemRoot, "userA");
        final Folder docs = persistFolder("docs", userARoot, "userA/docs");
        final Folder projects = persistFolder("projects", docs, "userA/docs/projects");
        final Folder sub = persistFolder("sub", projects, "userA/docs/projects/sub");
        final File report = persistFile("report.txt", projects, "userA/docs/projects/report.txt", "userA/docs/projects");
        final File nested = persistFile("nested.txt", sub, "userA/docs/projects/sub/nested.txt", "userA/docs/projects/sub");

        // --- 유저: 루트 폴더 = userA ---
        final User user = userRepository.save(User.builder().username(USERNAME).password("pw").build());
        user.updateRootFolder(userARoot);

        userARootId = userARoot.getId();
        docsId = docs.getId();
        projectsId = projects.getId();
        subId = sub.getId();
        reportId = report.getId();
        nestedFileId = nested.getId();

        // --- 디스크 트리 구성 ---
        mkdir("userA/docs/projects/sub");
        touch("userA/docs/projects/report.txt");
        touch("userA/docs/projects/sub/nested.txt");

        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("폴더 삭제 : 폴더를 지우면 하위 포함 DB 와 디스크에서 모두 사라진다")
    void deleteFolder_removesDbAndDisk() {
        // When : projects 삭제
        folderService.deleteFolder(projectsId, USERNAME);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then : DB 에서 폴더 자신 + 하위 폴더/파일이 모두 제거됨
        assertThat(folderRepository.findById(projectsId)).isEmpty();
        assertThat(folderRepository.findById(subId)).isEmpty();
        assertThat(fileRepository.findById(reportId)).isEmpty();
        assertThat(fileRepository.findById(nestedFileId)).isEmpty();

        // Then : 디스크에서도 제거됨 (부모 docs 는 유지)
        assertThat(Files.exists(tempRoot.resolve("userA/docs/projects"))).isFalse();
        assertThat(Files.exists(tempRoot.resolve("userA/docs"))).isTrue();
        assertThat(folderRepository.findById(docsId)).isPresent();
    }

    @Test
    @DisplayName("폴더 삭제 : DB 삭제가 실패하면 디스크 파일을 지우지 않는다")
    void deleteFolder_dbFailure_doesNotDeleteDisk() {
        // Given : userA 루트는 서비스에서 삭제를 막아야 한다
        //         → 예외가 나면 디스크를 건드리면 안 된다
        // When & Then : 루트 폴더 삭제 시도 → 예외 전파
        assertThatThrownBy(() -> folderService.deleteFolder(userARootId, USERNAME))
                .isInstanceOf(RuntimeException.class);

        // Then : 삭제가 실패했으므로 디스크 파일은 그대로 보존됨
        assertThat(Files.exists(tempRoot.resolve("userA/docs/projects/report.txt"))).isTrue();
        assertThat(Files.exists(tempRoot.resolve("userA/docs/projects/sub/nested.txt"))).isTrue();
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
