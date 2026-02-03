package com.seohamin.fshs.v2.domain.folder;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.share.repository.SharedFileRepository;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.config.JpaAuditingConfig;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class FolderRepositoryTest {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    @DisplayName("폴더 저장 : 필수 정보 포함된 폴더가 성공적으로 저장됨")
    void saveFolder_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");

        // When
        final Folder savedFolder = folderRepository.save(folder);
        testEntityManager.flush();

        // Then
        assertThat(savedFolder.getId()).isNotNull();
        assertThat(savedFolder.getName()).isEqualTo("folderName");
        assertThat(savedFolder.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("폴더 저장 : 유저와 루트 폴더의 연관관계가 성공적으로 저장됨")
    void saveUserWithRootFolder_Success() {
        // Given
        final User user = User.builder()
                .username("testUser")
                .password("testPassword")
                .build();
        final User savedUser = userRepository.save(user);

        final Folder rootFolder = createTestFolder("rootFolder");
        final Folder savedRootFolder = folderRepository.save(rootFolder);

        // When
        savedUser.updateRootFolder(savedRootFolder);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then
        final User foundUser = userRepository.findById(savedUser.getId()).get();
        final Folder foundFolder = folderRepository.findById(savedRootFolder.getId()).get();

        assertThat(foundUser.getRootFolder().getId()).isEqualTo(savedRootFolder.getId());
        assertThat(foundFolder.getOwner().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("폴더 저장 : 상위 폴더에 폴더가 성공적으로 저장됨")
    void saveFolderWithParentFolder_Success() {
        // Given
        final Folder parentFolder = createTestFolder("parentFolder");
        folderRepository.save(parentFolder);
        testEntityManager.flush();
        final Folder folder = createTestFolder("folderName");
        folder.updateParentFolder(parentFolder);

        // When
        final Folder savedFolder = folderRepository.save(folder);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then
        assertThat(savedFolder.getParentFolder().getName()).isEqualTo("parentFolder");
    }

    @Test
    @DisplayName("폴더 저장 : 중복 폴더가 있을 때 저장 실패함")
    void duplicateFolder_Failed() {
        // Given
        final Folder parentFolder = createTestFolder("parentFolder");
        folderRepository.save(parentFolder);
        testEntityManager.flush();
        final Folder folder = createTestFolder("folderName");
        folder.updateParentFolder(parentFolder);
        folderRepository.save(folder);
        testEntityManager.flush();
        final Folder duplicateFolder = createTestFolder("folderName");
        duplicateFolder.updateParentFolder(parentFolder);

        // When & Then
        assertThatThrownBy(() -> {
            folderRepository.save(duplicateFolder);
            testEntityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("폴더 저장 : DB에 들어갈 수 있는 길이 이상의 문자열은 실패함")
    void tooLongString_Fail() {
        // Given
        final Folder tooLongFolder = createTestFolder("/home/user/documents/university/2026/spring/comput/home/user/documents/university/2026/spring/comput/home/user/documents/university/2026/spring/comput/home/user/documents/university/2026/spring/comput/home/user/documents/university/2026/spring/comput/home/user/documents/university/2026/spring/comput");

        // When & Then
        assertThatThrownBy(() -> {
            folderRepository.save(tooLongFolder);
            testEntityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("폴더 저장 : 폴더명이 null인 폴더는 실패함")
    void noFolderName_Fail() {
        // Given
        final Folder noNameFolder = createTestFolder("folderName");
        noNameFolder.updateName(null);

        // When & Then
        assertThatThrownBy(() -> {
            folderRepository.save(noNameFolder);
            testEntityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("폴더 조회 : ID로 폴더 조회가 가능함")
    void findById_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");
        final Long folderId = folderRepository.save(folder).getId();
        testEntityManager.flush();
        testEntityManager.clear();

        // When
        final Optional<Folder> foundFolder = folderRepository.findById(folderId);

        // Then
        assertThat(foundFolder).isPresent();
        assertThat(foundFolder.get().getName()).isEqualTo("folderName");
    }

    @Test
    @DisplayName("폴더 수정 : 더티 체킹을 통한 정보 수정")
    void updateFolder_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");
        final Folder newParentFolder = createTestFolder("newParentFolder");
        testEntityManager.persist(newParentFolder);
        final Long folderId = folderRepository.save(folder).getId();
        testEntityManager.flush();
        final Instant now = Instant.now();

        // When
        final Folder foundFolder = folderRepository.findById(folderId).get();
        foundFolder.updateParentFolder(newParentFolder);
        foundFolder.updateOwnerId(5L);
        foundFolder.updateRelativePath("/newParentFolder/newName/");
        foundFolder.updateName("newName");
        foundFolder.updateOriginCreatedAt(now);
        foundFolder.updateOriginUpdatedAt(now);
        foundFolder.updateIsNfd(true);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then
        final Folder updatedFolder = folderRepository.findById(folderId).get();
        assertThat(updatedFolder.getParentFolder().getName()).isEqualTo("newParentFolder");
        assertThat(updatedFolder.getOwnerId()).isEqualTo(5L);
        assertThat(updatedFolder.getRelativePath()).isEqualTo("/newParentFolder/newName/");
        assertThat(updatedFolder.getName()).isEqualTo("newName");
        assertThat(updatedFolder.getOriginCreatedAt()).isCloseTo(now, within(1, ChronoUnit.SECONDS));
        assertThat(updatedFolder.getOriginUpdatedAt()).isCloseTo(now, within(1, ChronoUnit.SECONDS));
        assertThat(updatedFolder.getIsNfd()).isEqualTo(true);
    }

    @Test
    @DisplayName("폴더 수정 : DB 정보 수정 시간이 잘 반영 됨")
    void updateFolderUpdatedAt_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");
        final Long folderId = folderRepository.save(folder).getId();
        testEntityManager.flush();

        // When
        final Folder targetFolder = folderRepository.findById(folderId).get();
        targetFolder.updateName("newName");
        testEntityManager.flush();

        // Then
        final Folder updatedFolder = folderRepository.findById(folderId).get();
        assertThat(updatedFolder.getUpdatedAt()).isAfter(updatedFolder.getCreatedAt());
    }

    @Test
    @DisplayName("폴더 삭제 : 폴더 ID로 삭제 후 해당 ID로 조회시 결과 없음")
    void deleteById_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");
        final Long folderId = folderRepository.save(folder).getId();
        testEntityManager.flush();

        // When
        folderRepository.deleteById(folderId);
        testEntityManager.flush();

        // Then
        final Optional<Folder> foundFolder = folderRepository.findById(folderId);
        assertThat(foundFolder).isEmpty();
    }

    @Test
    @DisplayName("폴더 삭제 : 폴더 삭제시 하위 파일이 삭제됨")
    void deleteFolder_thenFileDeleted() {
        // Given
        final Folder folder = createTestFolder("folderName");
        final Long folderId = folderRepository.save(folder).getId();
        final File file = createTestFile(folder, "fileName", "jpg");
        final Long fileId = fileRepository.save(file).getId();
        testEntityManager.flush();
        testEntityManager.clear();

        // When
        folderRepository.deleteById(folderId);
        testEntityManager.flush();

        // Then
        assertThat(fileRepository.findById(fileId)).isEmpty();
    }

    @Test
    @DisplayName("폴더 삭제 : 폴더 삭제시 하위 폴더가 삭제됨")
    void deleteFolder_thenSubFolderDeleted() {
        // Given
        final Folder parentFolder = createTestFolder("parentFolder");
        final Long parentFolderId = folderRepository.save(parentFolder).getId();
        final Folder childFolder = createTestFolder("childFolder");
        childFolder.updateParentFolder(parentFolder);
        final Long childFolderId = folderRepository.save(childFolder).getId();
        testEntityManager.flush();
        testEntityManager.clear();

        // When
        folderRepository.deleteById(parentFolderId);
        testEntityManager.flush();

        // Then
        assertThat(folderRepository.findById(childFolderId)).isEmpty();
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
