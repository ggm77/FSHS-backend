package org.iptime.raspinas.FSHS.media.domain.port.out;

public interface FileConvertPort {

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
