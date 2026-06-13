package com.seohamin.fshs.v2.domain.file;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.file.service.FileService;
import com.seohamin.fshs.v2.domain.file.service.FileThumbnailProcessor;
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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FileServiceThumbnailTest {

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
    private FileThumbnailProcessor fileThumbnailProcessor;

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(
                fileRepository,
                folderRepository,
                userRepository,
                storageManager,
                fileUploadProcessor,
                Caffeine.newBuilder().build(),
                Caffeine.newBuilder().build(),
                Caffeine.newBuilder().build(),
                ffmpegProcessor,
                fileThumbnailProcessor
        );
    }

    @Test
    @DisplayName("파일 조회 응답에는 uuid가 포함된다")
    void fileResponseDto_includesUuid() {
        final File file = createFile("file-uuid", "userA/photos/photo.jpg");

        final FileResponseDto dto = FileResponseDto.of(file);

        assertThat(dto.uuid()).isEqualTo("file-uuid");
    }

    @Test
    @DisplayName("파일 UUID로 썸네일을 조회한다")
    void getFileThumbnail_returnsThumbnailResource() throws Exception {
        final String fileUuid = "file-uuid";
        final File file = createFile(fileUuid, "userA/photos/photo.jpg");
        final User user = createUser("tester", "userA");
        final Path thumbnailPath = tempDir.resolve(fileUuid + ".jpg");
        Files.writeString(thumbnailPath, "thumbnail");

        given(fileRepository.findByUuid(fileUuid)).willReturn(Optional.of(file));
        given(userRepository.findByUsername("tester")).willReturn(Optional.of(user));
        given(fileThumbnailProcessor.resolveThumbnailPath(fileUuid)).willReturn(thumbnailPath);

        final FileDownloadResponseDto dto = fileService.getFileThumbnail(fileUuid, "tester");

        assertThat(dto.name()).isEqualTo(fileUuid + ".jpg");
        assertThat(dto.mimeType()).isEqualTo("image/jpeg");
        assertThat(dto.size()).isEqualTo(Files.size(thumbnailPath));
        assertThat(dto.resource().exists()).isTrue();
    }

    @Test
    @DisplayName("권한 없는 파일의 썸네일은 조회할 수 없다")
    void getFileThumbnail_withoutPermission_throwsInvalidPath() {
        final String fileUuid = "file-uuid";
        final File file = createFile(fileUuid, "other/photos/photo.jpg");
        final User user = createUser("tester", "userA");

        given(fileRepository.findByUuid(fileUuid)).willReturn(Optional.of(file));
        given(userRepository.findByUsername("tester")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> fileService.getFileThumbnail(fileUuid, "tester"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.INVALID_PATH);
    }

    private File createFile(
            final String uuid,
            final String relativePath
    ) {
        return File.builder()
                .uuid(uuid)
                .name("photo.jpg")
                .baseName("photo")
                .extension("jpg")
                .relativePath(relativePath)
                .parentPath(Path.of(relativePath).getParent().toString())
                .mimeType("image/jpeg")
                .size(100L)
                .originUpdatedAt(Instant.parse("2026-06-13T00:00:00Z"))
                .category(Category.IMAGE)
                .build();
    }

    private User createUser(
            final String username,
            final String rootPath
    ) {
        final Folder rootFolder = Folder.builder()
                .name(rootPath)
                .relativePath(rootPath)
                .originUpdatedAt(Instant.parse("2026-06-13T00:00:00Z"))
                .isRoot(true)
                .build();
        final User user = User.builder()
                .username(username)
                .password("password")
                .build();
        user.updateRootFolder(rootFolder);
        return user;
    }
}
