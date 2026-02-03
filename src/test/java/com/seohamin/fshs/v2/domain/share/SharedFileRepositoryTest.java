package com.seohamin.fshs.v2.domain.share;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import com.seohamin.fshs.v2.domain.share.repository.SharedFileRepository;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.config.JpaAuditingConfig;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SharedFileRepositoryTest {

    @Autowired
    private SharedFileRepository sharedFileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FolderRepository folderRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    private User testUser;
    private File testFile;

    @BeforeEach
    void setUp() {
        // Given
        testUser = userRepository.save(createTestUser("testUser"));
        Folder testFolder = folderRepository.save(createTestFolder("testFolder", testUser.getId()));
        testFile = fileRepository.save(createTestFile(testFolder, "testFile", "txt"));
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("공유 파일 저장 : 필수 정보 포함된 공유 파일이 성공적으로 저장됨")
    void saveSharedFile_Success() {
        // Given
        User user = userRepository.findById(testUser.getId()).get();
        File file = fileRepository.findById(testFile.getId()).get();
        final SharedFile sharedFile = createTestSharedFile("test-key", file, user);

        // When
        final SharedFile savedSharedFile = sharedFileRepository.save(sharedFile);
        testEntityManager.flush();

        // Then
        assertThat(savedSharedFile.getId()).isNotNull();
        assertThat(savedSharedFile.getShareKey()).isEqualTo("test-key");
        assertThat(savedSharedFile.getFile().getId()).isEqualTo(file.getId());
        assertThat(savedSharedFile.getOwner().getId()).isEqualTo(user.getId());
        assertThat(savedSharedFile.getCreatedAt()).isNotNull();
        assertThat(savedSharedFile.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("공유 파일 저장 : 중복된 공유 키(shareKey)가 있을 때 저장 실패함")
    void duplicateShareKey_Fail() {
        // Given
        User user = userRepository.findById(testUser.getId()).get();
        File file = fileRepository.findById(testFile.getId()).get();
        sharedFileRepository.save(createTestSharedFile("duplicate-key", file, user));
        testEntityManager.flush();

        // When & Then
        final SharedFile duplicateSharedFile = createTestSharedFile("duplicate-key", file, user);
        assertThatThrownBy(() -> {
            sharedFileRepository.save(duplicateSharedFile);
            testEntityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("공유 파일 저장 : 공유 키(shareKey)가 null일 때 저장 실패함")
    void nullShareKey_Fail() {
        // Given
        User user = userRepository.findById(testUser.getId()).get();
        File file = fileRepository.findById(testFile.getId()).get();
        final SharedFile sharedFile = createTestSharedFile(null, file, user);

        // When & Then
        assertThatThrownBy(() -> {
            sharedFileRepository.saveAndFlush(sharedFile);
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("공유 파일 저장 : 파일(File)이 null일 때 저장 실패함")
    void nullFile_Fail() {
        // Given
        User user = userRepository.findById(testUser.getId()).get();
        final SharedFile sharedFile = createTestSharedFile("test-key", null, user);

        // When & Then
        assertThatThrownBy(() -> {
            sharedFileRepository.saveAndFlush(sharedFile);
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("공유 파일 저장 : 소유자(User)가 null일 때 저장 실패함")
    void nullOwner_Fail() {
        // Given
        File file = fileRepository.findById(testFile.getId()).get();
        final SharedFile sharedFile = createTestSharedFile("test-key", file, null);

        // When & Then
        assertThatThrownBy(() -> {
            sharedFileRepository.saveAndFlush(sharedFile);
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("공유 파일 조회 : ID로 공유 파일 조회가 가능함")
    void findById_Success() {
        // Given
        User user = userRepository.findById(testUser.getId()).get();
        File file = fileRepository.findById(testFile.getId()).get();
        final Long sharedFileId = sharedFileRepository.save(createTestSharedFile("test-key", file, user)).getId();
        testEntityManager.flush();
        testEntityManager.clear();

        // When
        final Optional<SharedFile> foundSharedFile = sharedFileRepository.findById(sharedFileId);

        // Then
        assertThat(foundSharedFile).isPresent();
        assertThat(foundSharedFile.get().getShareKey()).isEqualTo("test-key");
    }

    @Test
    @DisplayName("공유 파일 삭제 : 공유 파일 ID로 삭제 후 해당 ID로 조회시 결과 없음")
    void deleteById_Success() {
        // Given
        User user = userRepository.findById(testUser.getId()).get();
        File file = fileRepository.findById(testFile.getId()).get();
        final Long sharedFileId = sharedFileRepository.save(createTestSharedFile("test-key", file, user)).getId();
        testEntityManager.flush();

        // When
        sharedFileRepository.deleteById(sharedFileId);
        testEntityManager.flush();

        // Then
        final Optional<SharedFile> foundSharedFile = sharedFileRepository.findById(sharedFileId);
        assertThat(foundSharedFile).isEmpty();
    }

    @Test
    @DisplayName("공유 파일 삭제 : 연관된 유저(User) 삭제 시 공유 파일이 삭제됨")
    void deleteUser_thenSharedFileDeleted() {
        // Given
        User user = userRepository.findById(testUser.getId()).get();
        File file = fileRepository.findById(testFile.getId()).get();
        final Long sharedFileId = sharedFileRepository.save(createTestSharedFile("test-key", file, user)).getId();
        testEntityManager.flush();

        // When
        userRepository.deleteById(user.getId());
        testEntityManager.flush();

        // Then
        assertThat(sharedFileRepository.findById(sharedFileId)).isEmpty();
    }

    @Test
    @DisplayName("공유 파일 삭제 : 연관된 파일(File) 삭제 시 공유 파일이 삭제됨")
    void deleteFile_thenSharedFileDeleted() {
        // Given
        User user = userRepository.findById(testUser.getId()).get();
        File file = fileRepository.findById(testFile.getId()).get();
        final Long sharedFileId = sharedFileRepository.save(createTestSharedFile("test-key", file, user)).getId();
        testEntityManager.flush();

        // When
        fileRepository.deleteById(file.getId());
        testEntityManager.flush();

        // Then
        assertThat(sharedFileRepository.findById(sharedFileId)).isEmpty();
    }


    private SharedFile createTestSharedFile(final String shareKey, final File file, final User owner) {
        return SharedFile.builder()
                .shareKey(shareKey)
                .file(file)
                .owner(owner)
                .build();
    }

    private User createTestUser(final String username) {
        return User.builder()
                .username(username)
                .password("password")
                .build();
    }

    private Folder createTestFolder(final String name, final Long ownerId) {
        return Folder.builder()
                .parentFolder(null)
                .ownerId(ownerId)
                .relativePath("/"+name+"/")
                .name(name)
                .originCreatedAt(Instant.now())
                .originUpdatedAt(Instant.now())
                .isNfd(false)
                .build();
    }

    private File createTestFile(
            final Folder folder,
            final String baseName,
            final String extension
    ) {
        return File.builder()
                .parentFolder(folder)
                .ownerId(folder.getOwnerId())
                .name(baseName+"."+extension)
                .baseName(baseName)
                .extension(extension)
                .relativePath(folder.getRelativePath() + baseName + "." + extension)
                .parentPath(folder.getRelativePath())
                .mimeType("text/plain")
                .size(1L)
                .originCreatedAt(Instant.now())
                .originUpdatedAt(Instant.now())
                .category(Category.DOCUMENT)
                .isNfd(false)
                .build();
    }
}