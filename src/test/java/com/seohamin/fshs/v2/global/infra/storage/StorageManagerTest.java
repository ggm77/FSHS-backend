package com.seohamin.fshs.v2.global.infra.storage;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.global.infra.storage.dto.FilePropertiesDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class StorageManagerTest {

    @TempDir
    Path tempRoot;

    @Test
    @DisplayName("파일 속성 조회 : 알려진 확장자는 OS MIME 조회 없이 분류한다")
    void getFileProperties_knownExtension_skipsOsMimeProbe() {
        final StorageIoCore storageIoCore = mock(StorageIoCore.class);
        final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
        final Path path = tempRoot.resolve("movie.m4v");
        given(storageIoCore.readAttributes(path)).willReturn(attrs);
        given(attrs.size()).willReturn(10L);

        final StorageManager storageManager = new StorageManager(storageIoCore);
        final FilePropertiesDto properties = storageManager.getFileProperties(path);

        assertThat(properties.category()).isEqualTo(Category.VIDEO);
        assertThat(properties.mimeType()).isEqualTo("video/x-m4v");
        then(storageIoCore).should(never()).getMimeType(path);
    }

    @Test
    @DisplayName("파일 속성 조회 : 낯선 확장자는 MIME Type으로 카테고리를 보정한다")
    void getFileProperties_unknownExtension_usesMimeTypeFallbackForCategory() {
        final StorageIoCore storageIoCore = mock(StorageIoCore.class);
        final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
        final Path path = tempRoot.resolve("movie.custom");
        given(storageIoCore.getMimeType(path)).willReturn("video/mp4");
        given(storageIoCore.readAttributes(path)).willReturn(attrs);
        given(attrs.size()).willReturn(10L);

        final StorageManager storageManager = new StorageManager(storageIoCore);
        final FilePropertiesDto properties = storageManager.getFileProperties(path);

        assertThat(properties.category()).isEqualTo(Category.VIDEO);
        assertThat(properties.mimeType()).isEqualTo("video/mp4");
    }

    @Test
    @DisplayName("임시 파일 정리 : 임시 파일과 빈 UUID 디렉터리를 삭제한다")
    void deleteTemporaryFile_deletesFileAndParentDirectory() throws Exception {
        final Path tempPath = tempRoot.resolve("temp");
        final Path tempParent = tempPath.resolve("upload-id");
        final Path tempFile = tempParent.resolve("video.mp4");
        Files.createDirectories(tempParent);
        Files.writeString(tempFile, "content");

        final StorageManager storageManager = new StorageManager(new StorageIoCore());
        ReflectionTestUtils.setField(storageManager, "rootPath", tempRoot.resolve("data").toString());
        ReflectionTestUtils.setField(storageManager, "tempPath", tempPath.toString());

        storageManager.deleteTemporaryFile(tempFile);

        assertThat(Files.exists(tempFile)).isFalse();
        assertThat(Files.exists(tempParent)).isFalse();
        assertThat(Files.exists(tempPath)).isTrue();
    }
}
