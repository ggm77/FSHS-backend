package com.seohamin.fshs.v2.global.infra.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StorageManagerTest {

    @TempDir
    Path tempRoot;

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
