package com.seohamin.fshs.v2.domain.file;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.service.FileThumbnailProcessor;
import com.seohamin.fshs.v2.global.infra.ffmpeg.FfmpegProcessor;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;

@ExtendWith(MockitoExtension.class)
class FileThumbnailProcessorTest {

    @Mock
    private StorageManager storageManager;

    @Mock
    private FfmpegProcessor ffmpegProcessor;

    @TempDir
    Path tempDir;

    private FileThumbnailProcessor fileThumbnailProcessor;

    @BeforeEach
    void setUp() {
        fileThumbnailProcessor = new FileThumbnailProcessor(storageManager, ffmpegProcessor);
        ReflectionTestUtils.setField(
                fileThumbnailProcessor,
                "thumbnailPath",
                tempDir.resolve("thumbnails").toString()
        );
    }

    @Test
    @DisplayName("이미지 썸네일은 FFmpeg를 쓰지 않고 이미지 라이브러리로 생성한다")
    void process_image_usesImageLibrary() throws Exception {
        final String fileUuid = "image-uuid";
        final Path sourcePath = tempDir.resolve("source.png");
        writeImage(sourcePath);
        given(storageManager.resolvePath("user/source.png", false)).willReturn(sourcePath);

        fileThumbnailProcessor.process(fileUuid, "user/source.png", Category.IMAGE);

        assertThat(Files.exists(tempDir.resolve("thumbnails").resolve(fileUuid + ".jpg"))).isTrue();
        then(ffmpegProcessor).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("동영상 썸네일은 FfmpegProcessor로 위임한다")
    void process_video_delegatesToFfmpegProcessor() throws Exception {
        final String fileUuid = "video-uuid";
        final Path sourcePath = tempDir.resolve("source.mp4");
        Files.writeString(sourcePath, "video");
        given(storageManager.resolvePath("user/source.mp4", false)).willReturn(sourcePath);
        willAnswer(invocation -> {
            final Path targetPath = invocation.getArgument(1);
            Files.writeString(targetPath, "thumbnail");
            return null;
        }).given(ffmpegProcessor).createVideoThumbnail(eq(sourcePath), any(Path.class), eq(480));

        fileThumbnailProcessor.process(fileUuid, "user/source.mp4", Category.VIDEO);

        assertThat(Files.exists(tempDir.resolve("thumbnails").resolve(fileUuid + ".jpg"))).isTrue();
        then(ffmpegProcessor).should()
                .createVideoThumbnail(eq(sourcePath), any(Path.class), eq(480));
    }

    private void writeImage(final Path path) throws Exception {
        final BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", path.toFile());
    }
}
