package com.seohamin.fshs.v2.domain.file;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import com.seohamin.fshs.v2.domain.share.repository.SharedFileRepository;
import com.seohamin.fshs.v2.domain.user.entity.User;
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
import org.springframework.orm.jpa.JpaSystemException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class FileRepositoryTest {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private SharedFileRepository sharedFileRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    @DisplayName("파일 저장 : 필수 정보가 포함된 파일이 성공적으로 저장됨")
    void saveFile_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final File file = createTestFile(folder, "fileName", "jpg");

        // When
        final File savedFile = fileRepository.save(file);
        testEntityManager.flush();

        // Then
        assertThat(savedFile.getId()).isNotNull();
        assertThat(savedFile.getParentFolder().getName()).isEqualTo("folderName");
        assertThat(savedFile.getName()).isEqualTo("fileName.jpg");
        assertThat(savedFile.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("파일 저장 : 중복 파일이 있을 때 저장 실패함")
    void duplicateFile_Fail() {
        // Given
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final File file = createTestFile(folder, "fileName", "jpg");
        fileRepository.save(file);
        testEntityManager.flush();

        final File duplicateFile = createTestFile(folder, "fileName", "jpg");
        testEntityManager.clear();

        // When & Then
        assertThatThrownBy(() -> {
            fileRepository.save(duplicateFile);
            testEntityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("파일 저장 : DB에 들어갈 수 있는 길이 이상의 문자열은 실패함")
    void tooLongString_Fail() {
        // Given
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final File tooLongFile = createTestFile(folder, "fileName", "/home/user/documents/university/2026/spring/computer-science/project/v2/file.txt");

        // When & Then
        assertThatThrownBy(() -> {
            fileRepository.save(tooLongFile);
            testEntityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("파일 저장 : 파일명이 null인 파일은 실패함")
    void noFileName_Fail() {
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final File noNameFile = createTestFile(folder, "fileName", "jpg");
        noNameFile.updateName(null);

        // When & Then
        assertThatThrownBy(() -> {
            fileRepository.save(noNameFile);
            testEntityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("파일 조회 : 연관된 폴더와 함께 파일 조회가 가능함")
    void findById_WithFolder_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final File file = createTestFile(folder, "fileName", "jpg");
        final Long fileId = fileRepository.save(file).getId();

        // When
        final Optional<File> foundFile = fileRepository.findById(fileId);

        // Then
        assertThat(foundFile).isPresent();
        assertThat(foundFile.get().getBaseName()).isEqualTo("fileName");
        assertThat(foundFile.get().getParentFolder().getName()).isEqualTo("folderName");
    }

    @Test
    @DisplayName("파일 수정 : 더티 체킹을 통한 정보 수정")
    void updateFile_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final Folder newFolder = createTestFolder("newFolderName");
        testEntityManager.persist(newFolder);
        final File file = createTestFile(folder, "fileName", "jpg");
        final Long fileId = fileRepository.save(file).getId();
        testEntityManager.flush();
        final Instant now = Instant.now();

        // When
        final File foundFile = fileRepository.findById(fileId).get();
        foundFile.updateParentFolder(newFolder);
        foundFile.updateOwnerId(32L);
        foundFile.updateName("newName.mp4");
        foundFile.updateBaseName("newName");
        foundFile.updateExtension("mp4");
        foundFile.updateRelativePath(newFolder.getRelativePath() + "newName.mp4");
        foundFile.updateParentPath(newFolder.getRelativePath());
        foundFile.updateMimeType("video/mp4");
        foundFile.updateSize(12345L);
        foundFile.updateVideoCodec("newVideoCodec");
        foundFile.updateAudioCodec("newAudioCodec");
        foundFile.updateDuration(67L);
        foundFile.updateOrientation(-90);
        foundFile.updateOriginCreatedAt(now);
        foundFile.updateOriginUpdatedAt(now);
        foundFile.updateCategory(Category.VIDEO);
        foundFile.updateIsNfd(true);

        fileRepository.flush();
        testEntityManager.clear();

        // Then
        final File updatedFile = fileRepository.findById(fileId).get();
        assertThat(updatedFile.getParentFolder().getName()).isEqualTo(newFolder.getName());
        assertThat(updatedFile.getOwnerId()).isEqualTo(32L);
        assertThat(updatedFile.getName()).isEqualTo("newName.mp4");
        assertThat(updatedFile.getBaseName()).isEqualTo("newName");
        assertThat(updatedFile.getExtension()).isEqualTo("mp4");
        assertThat(updatedFile.getRelativePath()).isEqualTo(newFolder.getRelativePath() + "newName.mp4");
        assertThat(updatedFile.getParentPath()).isEqualTo(newFolder.getRelativePath());
        assertThat(updatedFile.getMimeType()).isEqualTo("video/mp4");
        assertThat(updatedFile.getSize()).isEqualTo(12345L);
        assertThat(updatedFile.getVideoCodec()).isEqualTo("newVideoCodec");
        assertThat(updatedFile.getAudioCodec()).isEqualTo("newAudioCodec");
        assertThat(updatedFile.getDuration()).isEqualTo(67L);
        assertThat(updatedFile.getOrientation()).isEqualTo(-90);
        assertThat(updatedFile.getOriginCreatedAt()).isCloseTo(now, within(1, ChronoUnit.SECONDS));
        assertThat(updatedFile.getOriginUpdatedAt()).isCloseTo(now, within(1, ChronoUnit.SECONDS));
        assertThat(updatedFile.getCategory()).isEqualTo(Category.VIDEO);
        assertThat(updatedFile.getIsNfd()).isEqualTo(true);
    }

    @Test
    @DisplayName("파일 수정 : DB 정보 수정 시간이 잘 반영 됨")
    void updateFolderUpdatedAt_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final File file = createTestFile(folder, "fileName", "jpg");
        final Long fileId = fileRepository.save(file).getId();
        testEntityManager.flush();

        // When
        final File targetFile = fileRepository.findById(fileId).get();
        targetFile.updateOwnerId(99L);
        testEntityManager.flush();

        // Then
        final File updatedFile = fileRepository.findById(fileId).get();
        assertThat(updatedFile.getUpdatedAt()).isAfter(updatedFile.getCreatedAt());
    }

    @Test
    @DisplayName("파일 삭제 : 파일 ID로 삭제 후 해당 ID로 조회시 결과 없음")
    void deleteById_Success() {
        // Given
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final File file = createTestFile(folder, "fileName", "jpg");
        final Long fileId = fileRepository.save(file).getId();

        // When
        fileRepository.deleteById(fileId);
        testEntityManager.flush();

        // Then
        final Optional<File> foundFile = fileRepository.findById(fileId);
        assertThat(foundFile).isEmpty();
    }

    @Test
    @DisplayName("파일 삭제 : 파일 삭제시 파일 공유 정보가 삭제됨")
    void deleteFile_ThenSharedFileDeleted() {
        // Given
        final Folder folder = createTestFolder("folderName");
        testEntityManager.persist(folder);
        final File file = createTestFile(folder, "fileName", "jpg");
        final User user = User.builder()
                .username("username")
                .password("password")
                .build();
        testEntityManager.persist(user);
        final Long fileId = fileRepository.save(file).getId();
        final SharedFile sharedFile = SharedFile.builder()
                .shareKey("key")
                .file(file)
                .owner(user)
                .build();
        final Long sharedFileId = sharedFileRepository.save(sharedFile).getId();
        testEntityManager.flush();
        testEntityManager.clear();

        // When
        fileRepository.deleteById(fileId);
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
