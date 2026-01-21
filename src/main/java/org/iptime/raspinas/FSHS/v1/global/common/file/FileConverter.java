package org.iptime.raspinas.FSHS.v1.global.common.file;

public interface FileConverter {

    void videoToHls(
            final String filePath,
            final String hlsPath,
            final String fileName
    );

    void audioToHls(
            final String filePath,
            final String hlsPath,
            final String fileName
    );

    void thumbnail(
            final String filePath,
            final String thumbnailPath
    );

    void albumCoverImage(
            final String filePath,
            final String thumbnailPath,
            final String fileName
    );
}
