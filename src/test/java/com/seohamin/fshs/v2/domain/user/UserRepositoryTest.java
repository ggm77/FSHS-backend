package com.seohamin.fshs.v2.domain.user;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import com.seohamin.fshs.v2.domain.share.repository.SharedFileRepository;
import com.seohamin.fshs.v2.domain.user.entity.Role;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SharedFileRepository sharedFileRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    @DisplayName("유저 저장 : 유저 ID가 생성됨")
    void savedUserHasId() {
        // Given
        final User user = new User("test", "test");

        // When
        final User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
    }

    @Test
    @DisplayName("유저 저장 : 저장된 username이 요청한 값과 일치함")
    void savedUserUsernameSame() {
        // Given
        final User user = new User("test", "test");

        // When
        final User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("유저 저장 : 생성 시점이 저장됨")
    void savedUserCreatedAt() {
        // Given
        final User user = new User("test", "test");

        // When
        final User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("유저 조회 : 유저 ID로 유저 조회가 가능함")
    void findById_Success() {
        // Given
        final User user = new User("test", "test");
        final User savedUser = userRepository.save(user);
        final Long savedUserId = savedUser.getId();

        // When
        final Optional<User> foundUser = userRepository.findById(savedUserId);

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("유저 조회 : 존재하지 않는 ID로 조회시 빈 Optional 반환")
    void findById_NotFound() {
        // Given
        final Long userId = 999999L;

        // When
        final Optional<User> foundUser = userRepository.findById(userId);

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("유저 조회 : username으로 유저 조회가 가능함")
    void findByUsername_Success() {
        // Given
        final User user = new User("test", "test");
        userRepository.save(user);

        // When
        final Optional<User> foundUser = userRepository.findByUsername("test");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("유저 조회 : 존재하지 않는 username으로 조회시 빈 Optional 반환")
    void findByUsername_NotFound() {
        // Given
        final String username = "non-existent";

        // When
        final Optional<User> foundUser = userRepository.findByUsername(username);

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("유저 업데이트 : 더티 체킹을 통한 데이터 업데이트 확인")
    void updateWithDirtyChecking() {
        // Given
        final User user = new User("oldUsername", "oldPassword");
        final User savedUser = userRepository.save(user);
        final Long savedUserId = savedUser.getId();

        // When
        savedUser.updateUsername("newUsername");
        savedUser.updatePassword("newPassword");
        savedUser.updateRole(Role.ADMIN);
        userRepository.flush();
        testEntityManager.clear();

        // Then
        final Optional<User> foundUser = userRepository.findById(savedUserId);
        assertThat(foundUser.get().getUsername()).isEqualTo("newUsername");
        assertThat(foundUser.get().getPassword()).isEqualTo("newPassword");
        assertThat(foundUser.get().getUserRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("유저 수정 : Role을 null로 수정시 실패함")
    void updateWithNullRole() {
        // Given
        final User user = new User("username", "password");
        final User savedUser = userRepository.save(user);
        final Instant createdAt = savedUser.getCreatedAt();
        final Long userId = savedUser.getId();
        testEntityManager.flush();

        // When & Then
        final User targetUser = userRepository.findById(userId).get();
        assertThatThrownBy(() -> {
            targetUser.updateRole(null);
            testEntityManager.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("유저 업데이트 : 정보 수정시 updatedAt 수정됨")
    void updateUpdatedAt() {
        // Given
        final User user = new User("username", "password");
        final User savedUser = userRepository.save(user);
        final Instant createdAt = savedUser.getCreatedAt();
        final Long userId = savedUser.getId();
        testEntityManager.flush();

        // When
        final User targetUser = userRepository.findById(userId).get();
        targetUser.updateUsername("newUsername");
        testEntityManager.flush();
        testEntityManager.clear();

        // Then
        final User updatedUser = userRepository.findById(userId).get();
        assertThat(updatedUser.getUpdatedAt()).isAfter(createdAt);
    }

    @Test
    @DisplayName("유저 삭제 : 유저 ID로 유저 삭제가 가능함")
    void deleteById_Success() {
        // Given
        final User user = new User("test", "test");
        final User savedUser = userRepository.save(user);
        final Long savedUserId = savedUser.getId();

        // When
        userRepository.deleteById(savedUserId);

        // Then
        final Optional<User> foundUser = userRepository.findById(savedUserId);
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("유저 삭제 : 유저 삭제시 연관된 공유된 파일 정보 삭제됨")
    void deleteUser_ThenSharedFileDeleted() {
        // Given
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final File file = createTestFile(folder, "fileName", "jpg");
        testEntityManager.persist(file);
        final User user = new User("test", "test");
        final Long userId = userRepository.save(user).getId();
        testEntityManager.flush();
        testEntityManager.clear();
        final SharedFile sharedFile = SharedFile.builder()
                .shareKey("key")
                .file(file)
                .owner(user)
                .build();
        final Long sharedFileId = sharedFileRepository.save(sharedFile).getId();
        testEntityManager.flush();
        testEntityManager.clear();

        // When
        userRepository.deleteById(userId);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then
        assertThat(sharedFileRepository.findById(sharedFileId)).isEmpty();
    }

    private Folder createTestFolder(final String name) {
        return Folder.builder()
                .parentFolder(null)
                .ownerId(null)
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
                .ownerId(null)
                .name(baseName+"."+extension)
                .baseName(baseName)
                .extension(extension)
                .relativePath(folder.getRelativePath() + baseName + "." + extension)
                .parentPath(folder.getRelativePath())
                .mimeType("image/jpeg")
                .size(1L)
                .videoCodec("videoCodec")
                .audioCodec("audioCodec")
                .width(2)
                .height(3)
                .duration(4L)
                .orientation(0)
                .originCreatedAt(Instant.now())
                .originUpdatedAt(Instant.now())
                .category(Category.IMAGE)
                .isNfd(false)
                .build();
    }
}
