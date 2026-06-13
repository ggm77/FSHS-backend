package com.seohamin.fshs.v2.domain.file;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seohamin.fshs.v2.domain.file.entity.Status;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.file.service.FileService;
import com.seohamin.fshs.v2.domain.file.service.FileUploadProcessor;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.ffmpeg.FfmpegProcessor;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class FileServiceUploadTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageManager storageManager;

    @Mock
    private FileUploadProcessor fileUploadProcessor;

    @Mock
    private FfmpegProcessor ffmpegProcessor;

    @Mock
    private MultipartFile multipartFile;

    private Cache<String, Status> fileStatusCache;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileStatusCache = Caffeine.newBuilder().build();
        fileService = new FileService(
                fileRepository,
                folderRepository,
                userRepository,
                storageManager,
                fileUploadProcessor,
                Caffeine.newBuilder().build(),
                fileStatusCache,
                Caffeine.newBuilder().build(),
                ffmpegProcessor
        );
    }

    @Test
    @DisplayName("파일 업로드 : 비동기 처리 큐가 가득 차면 임시 파일을 정리하고 ERROR 상태로 바꾼다")
    void uploadFile_asyncRejected_cleansTempFileAndMarksError() {
        final Long folderId = 1L;
        final Instant lastModified = Instant.parse("2026-06-13T00:00:00Z");
        final Path tempFilePath = Path.of("/tmp/fshs-temp/upload-id/video.mp4");
        final Folder userRoot = Folder.builder()
                .name("userA")
                .relativePath("userA")
                .originUpdatedAt(lastModified)
                .isRoot(true)
                .build();
        final Folder uploadFolder = Folder.builder()
                .name("uploads")
                .relativePath("userA/uploads")
                .originUpdatedAt(lastModified)
                .isRoot(false)
                .build();
        final User user = User.builder()
                .username("tester")
                .password("password")
                .build();
        user.updateRootFolder(userRoot);

        given(userRepository.findByUsername("tester")).willReturn(Optional.of(user));
        given(folderRepository.findById(folderId)).willReturn(Optional.of(uploadFolder));
        given(storageManager.saveTemporarily(multipartFile)).willReturn(tempFilePath);
        willThrow(new TaskRejectedException("queue full"))
                .given(fileUploadProcessor)
                .process(anyString(), eq(tempFilePath), eq(folderId), eq(lastModified));

        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, lastModified, folderId, "tester"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.UPLOAD_CAPACITY_EXCEEDED);

        assertThat(fileStatusCache.asMap()).hasSize(1);
        assertThat(fileStatusCache.asMap().values()).containsExactly(Status.ERROR);
        then(storageManager).should().deleteTemporaryFile(tempFilePath);
    }
}
