package com.seohamin.fshs.v2.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MimeTypeUtilTest {

    @Test
    @DisplayName("MIME Type 조회 : 확장자를 정규화해서 조회한다")
    void getMimeType_normalizesExtension() {
        assertThat(MimeTypeUtil.getMimeType(".JPG")).isEqualTo("image/jpeg");
        assertThat(MimeTypeUtil.getMimeType(" M4V ")).isEqualTo("video/x-m4v");
    }

    @Test
    @DisplayName("MIME Type 조회 : 대표 미디어, 문서, 압축 파일 MIME Type을 반환한다")
    void getMimeType_knownExtensions_returnsMimeType() {
        assertThat(MimeTypeUtil.getMimeType("heif")).isEqualTo("image/heif");
        assertThat(MimeTypeUtil.getMimeType("opus")).isEqualTo("audio/opus");
        assertThat(MimeTypeUtil.getMimeType("csv")).isEqualTo("text/csv");
        assertThat(MimeTypeUtil.getMimeType("hwp")).isEqualTo("application/x-hwp");
        assertThat(MimeTypeUtil.getMimeType("hwpx")).isEqualTo("application/vnd.hancom.hwpx");
        assertThat(MimeTypeUtil.getMimeType("tgz")).isEqualTo("application/gzip");
    }

    @Test
    @DisplayName("MIME Type 조회 : 알 수 없는 확장자는 null을 반환한다")
    void getMimeType_unknownExtension_returnsNull() {
        assertThat(MimeTypeUtil.getMimeType("unknown")).isNull();
        assertThat(MimeTypeUtil.getMimeType(null)).isNull();
    }
}
