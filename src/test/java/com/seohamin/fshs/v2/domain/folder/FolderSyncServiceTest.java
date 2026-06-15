package com.seohamin.fshs.v2.domain.folder;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.file.service.FileThumbnailProcessor;
import com.seohamin.fshs.v2.domain.folder.dto.FolderSyncResponseDto;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.folder.service.FolderSyncService;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.config.JpaAuditingConfig;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.FileAnalyzer;
import com.seohamin.fshs.v2.global.infra.storage.StorageIoCore;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import com.seohamin.fshs.v2.global.infra.storage.dto.FileAnalysisResultDto;
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
import java.nio.file.attribute.FileTime;
import java.text.Normalizer;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DataJpaTest
@Import({JpaAuditingConfig.class, SystemRootInitializer.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FolderSyncServiceTest {

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

    private FolderSyncService folderSyncService;
    private FileThumbnailProcessor fileThumbnailProcessor;

    private static final String USERNAME = "tester";

    private Long docsId;
    private Long archiveId;

    @BeforeEach
    void setUp() throws IOException {
        final StorageManager storageManager = new StorageManager(new StorageIoCore());
        ReflectionTestUtils.setField(storageManager, "rootPath", tempRoot.toString());
        ReflectionTestUtils.setField(storageManager, "tempPath", tempRoot.resolve("tmp").toString());

        final FileAnalyzer fileAnalyzer = new FileAnalyzer(null, null, storageManager);
        fileThumbnailProcessor = mock(FileThumbnailProcessor.class);
        folderSyncService = new FolderSyncService(
                folderRepository,
                fileRepository,
                userRepository,
                storageManager,
                fileAnalyzer,
                Caffeine.newBuilder().build(),
                Caffeine.newBuilder().build(),
                fileThumbnailProcessor
        );

        if (folderRepository.findById(1L).isEmpty()) {
            folderRepository.insertSystemRoot("data", "data");
        }
        final Folder systemRoot = folderRepository.findById(1L).get();

        final Folder userRoot = persistFolder("userA", systemRoot, "userA");
        final Folder docs = persistFolder("docs", userRoot, "userA/docs");
        final Folder archive = persistFolder("archive", userRoot, "userA/archive");
        final Folder staleFolder = persistFolder("stale", docs, "userA/docs/stale");

        persistFile("existing.txt", docs, "userA/docs/existing.txt", "userA/docs", 1L, Instant.parse("2026-06-01T00:00:00Z"));
        persistFile("old.txt", staleFolder, "userA/docs/stale/old.txt", "userA/docs/stale", 1L, Instant.parse("2026-06-01T00:00:00Z"));
        persistFile("outside.txt", archive, "userA/archive/outside.txt", "userA/archive", 1L, Instant.parse("2026-06-01T00:00:00Z"));

        final User user = userRepository.save(User.builder().username(USERNAME).password("pw").build());
        user.updateRootFolder(userRoot);

        docsId = docs.getId();
        archiveId = archive.getId();

        mkdir("userA/docs/new");
        mkdir("userA/archive");
        write("userA/docs/existing.txt", "changed", Instant.parse("2026-06-10T00:00:00Z"));
        write("userA/docs/new/new.txt", "new", Instant.parse("2026-06-11T00:00:00Z"));
        write("userA/archive/outside.txt", "x", Instant.parse("2026-06-12T00:00:00Z"));

        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("폴더 동기화 : 지정 폴더 하위의 추가/수정/삭제만 DB에 반영한다")
    void syncFolder_updatesOnlyTargetSubtree() {
        final FolderSyncResponseDto response = folderSyncService.syncFolder(docsId, USERNAME);
        testEntityManager.flush();
        testEntityManager.clear();

        assertThat(response.createdFolders()).containsExactly("userA/docs/new");
        assertThat(response.createdFiles()).containsExactly("userA/docs/new/new.txt");
        assertThat(response.updatedFiles()).containsExactly("userA/docs/existing.txt");
        assertThat(response.deletedFiles()).containsExactly("userA/docs/stale/old.txt");
        assertThat(response.deletedFolders()).containsExactly("userA/docs/stale");
        assertThat(response.errors()).isEmpty();

        final File existing = fileRepository.findAll().stream()
                .filter(file -> file.getRelativePath().equals("userA/docs/existing.txt"))
                .findFirst()
                .orElseThrow();
        assertThat(existing.getSize()).isEqualTo(7L);
        assertThat(existing.getCategory()).isEqualTo(Category.DOCUMENT);

        assertThat(folderRepository.findByRelativePath("userA/docs/new")).isPresent();
        assertThat(fileRepository.findAll().stream()
                .anyMatch(file -> file.getRelativePath().equals("userA/docs/new/new.txt"))).isTrue();
        assertThat(folderRepository.findByRelativePath("userA/docs/stale")).isEmpty();
        assertThat(fileRepository.findAll().stream()
                .anyMatch(file -> file.getRelativePath().equals("userA/docs/stale/old.txt"))).isFalse();

        assertThat(fileRepository.findAll().stream()
                .anyMatch(file -> file.getRelativePath().equals("userA/archive/outside.txt"))).isTrue();
    }

    @Test
    @DisplayName("폴더 동기화 : 접근 권한이 없는 폴더는 거부한다")
    void syncFolder_withoutPermission_throws() {
        final User limitedUser = userRepository.save(User.builder().username("limited").password("pw").build());
        final Folder docs = folderRepository.findById(docsId).orElseThrow();
        limitedUser.updateRootFolder(docs);
        testEntityManager.flush();
        testEntityManager.clear();

        assertThatThrownBy(() -> folderSyncService.syncFolder(archiveId, "limited"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getExceptionCode())
                .isEqualTo(ExceptionCode.INVALID_PATH);
    }

    @Test
    @DisplayName("폴더 동기화 : .DS_Store 파일은 생성하지 않고 기존 DB 정보도 제거한다")
    void syncFolder_ignoresDsStore() throws IOException {
        final Folder docs = folderRepository.findById(docsId).orElseThrow();
        persistFile(".DS_Store", docs, "userA/docs/.DS_Store", "userA/docs", 1L, Instant.parse("2026-06-01T00:00:00Z"));
        write("userA/docs/.DS_Store", "metadata", Instant.parse("2026-06-12T00:00:00Z"));
        write("userA/docs/new/.DS_Store", "metadata", Instant.parse("2026-06-12T00:00:00Z"));
        testEntityManager.flush();
        testEntityManager.clear();

        final FolderSyncResponseDto response = folderSyncService.syncFolder(docsId, USERNAME);
        testEntityManager.flush();
        testEntityManager.clear();

        assertThat(response.createdFiles()).doesNotContain("userA/docs/.DS_Store", "userA/docs/new/.DS_Store");
        assertThat(response.updatedFiles()).doesNotContain("userA/docs/.DS_Store", "userA/docs/new/.DS_Store");
        assertThat(response.deletedFiles()).contains("userA/docs/.DS_Store");
        assertThat(fileRepository.findAll().stream()
                .noneMatch(file -> file.getName().equals(".DS_Store"))).isTrue();
    }

    @Test
    @DisplayName("폴더 동기화 : 신규 이미지 파일 저장 후 썸네일 생성을 요청한다")
    void syncFolder_createdImageFile_requestsThumbnail() throws IOException {
        final FileAnalyzer fileAnalyzer = mock(FileAnalyzer.class);
        final FileThumbnailProcessor thumbnailProcessor = mock(FileThumbnailProcessor.class);
        given(fileAnalyzer.analyzeFile(any(Path.class))).willReturn(new FileAnalysisResultDto(
                "none",
                "none",
                null,
                "none",
                null,
                null,
                null,
                100,
                100,
                null,
                null,
                null,
                "image/jpeg",
                5L,
                Category.IMAGE,
                "photo.jpg",
                "photo",
                "jpg"
        ));

        final FolderSyncService syncService = new FolderSyncService(
                folderRepository,
                fileRepository,
                userRepository,
                testStorageManager(),
                fileAnalyzer,
                Caffeine.newBuilder().build(),
                Caffeine.newBuilder().build(),
                thumbnailProcessor
        );
        final Folder userRoot = folderRepository.findByRelativePath("userA").orElseThrow();
        final Folder images = persistFolder("images", userRoot, "userA/images");
        write("userA/images/photo.jpg", "photo", Instant.parse("2026-06-12T00:00:00Z"));
        testEntityManager.flush();
        testEntityManager.clear();

        syncService.syncFolder(images.getId(), USERNAME);

        then(thumbnailProcessor).should()
                .process(any(String.class), eq("userA/images/photo.jpg"), eq(Category.IMAGE));
    }

    @Test
    @DisplayName("폴더 동기화 : NFD 파일 경로는 보존하고 파일명은 NFC로 저장한다")
    void syncFolder_preservesNfdPathAndNormalizesFileName() throws IOException {
        final String nfcName = "한글.txt";
        final String nfdName = Normalizer.normalize(nfcName, Normalizer.Form.NFD);
        final String nfdPath = "userA/docs/" + nfdName;

        write(nfdPath, "korean", Instant.parse("2026-06-12T00:00:00Z"));
        testEntityManager.flush();
        testEntityManager.clear();

        folderSyncService.syncFolder(docsId, USERNAME);
        testEntityManager.flush();
        testEntityManager.clear();

        final File syncedFile = fileRepository.findAll().stream()
                .filter(file -> file.getRelativePath().equals(nfdPath))
                .findFirst()
                .orElseThrow();

        assertThat(syncedFile.getName()).isEqualTo(nfcName);
        assertThat(syncedFile.getBaseName()).isEqualTo("한글");
        assertThat(syncedFile.getRelativePath()).isEqualTo(nfdPath);
        assertThat(syncedFile.getRelativePath()).isNotEqualTo("userA/docs/" + nfcName);
        assertThat(syncedFile.getParentPath()).isEqualTo("userA/docs");
    }

    private Folder persistFolder(
            final String name,
            final Folder parent,
            final String relativePath
    ) {
        return folderRepository.save(Folder.builder()
                .parentFolder(parent)
                .ownerId(null)
                .relativePath(relativePath)
                .name(name)
                .originUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"))
                .isRoot(false)
                .build());
    }

    private File persistFile(
            final String name,
            final Folder parent,
            final String relativePath,
            final String parentPath,
            final Long size,
            final Instant originUpdatedAt
    ) {
        final int dot = name.lastIndexOf('.');
        final String base = dot > 0 ? name.substring(0, dot) : name;
        final String ext = dot > 0 ? name.substring(dot + 1) : "";
        return fileRepository.save(File.builder()
                .parentFolder(parent)
                .ownerId(null)
                .uuid(UUID.randomUUID().toString())
                .name(name)
                .baseName(base)
                .extension(ext)
                .relativePath(relativePath)
                .parentPath(parentPath)
                .mimeType("text/plain")
                .size(size)
                .originUpdatedAt(originUpdatedAt)
                .category(Category.DOCUMENT)
                .build());
    }

    private void mkdir(final String relativePath) throws IOException {
        Files.createDirectories(tempRoot.resolve(relativePath));
    }

    private void write(
            final String relativePath,
            final String content,
            final Instant lastModified
    ) throws IOException {
        final Path path = tempRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        Files.setLastModifiedTime(path, FileTime.from(lastModified));
    }

    private StorageManager testStorageManager() {
        final StorageManager storageManager = new StorageManager(new StorageIoCore());
        ReflectionTestUtils.setField(storageManager, "rootPath", tempRoot.toString());
        ReflectionTestUtils.setField(storageManager, "tempPath", tempRoot.resolve("tmp").toString());
        return storageManager;
    }
}
