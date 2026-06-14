package com.seohamin.fshs.v2.global.infra.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;

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

    @Test
    @DisplayName("파일 삭제 : DB 경로가 NFC여도 디스크에 NFD 파일명이 있으면 찾아서 삭제한다")
    void removeFile_nfcPathDeletesNfdDiskFile() throws Exception {
        final Path dataPath = tempRoot.resolve("data");
        Files.createDirectories(dataPath);
        final String nfcName = "성경의이해6_중간기.pdf";
        final String nfdName = Normalizer.normalize(nfcName, Normalizer.Form.NFD);
        final Path diskFile = dataPath.resolve(nfdName);
        Files.writeString(diskFile, "content");

        final StorageManager storageManager = new StorageManager(new StorageIoCore());
        ReflectionTestUtils.setField(storageManager, "rootPath", dataPath.toString());
        ReflectionTestUtils.setField(storageManager, "tempPath", tempRoot.resolve("temp").toString());

        storageManager.removeFile(nfcName);

        assertThat(Files.exists(diskFile)).isFalse();
    }
}
