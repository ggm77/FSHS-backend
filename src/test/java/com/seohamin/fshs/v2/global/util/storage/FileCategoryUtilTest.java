package com.seohamin.fshs.v2.global.util.storage;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileCategoryUtilTest {

    @Test
    @DisplayName("파일 카테고리 분류 : 알려진 확장자는 카테고리로 분류한다")
    void categorize_knownExtensions_returnsCategory() {
        assertThat(FileCategoryUtil.categorize(".JPG")).isEqualTo(Category.IMAGE);
        assertThat(FileCategoryUtil.categorize("m4v")).isEqualTo(Category.VIDEO);
        assertThat(FileCategoryUtil.categorize("opus")).isEqualTo(Category.AUDIO);
        assertThat(FileCategoryUtil.categorize("csv")).isEqualTo(Category.DOCUMENT);
        assertThat(FileCategoryUtil.categorize("tgz")).isEqualTo(Category.ARCHIVE);
    }

    @Test
    @DisplayName("파일 카테고리 분류 : 알려진 확장자는 MIME Type보다 우선한다")
    void categorize_knownExtension_takesPriorityOverMimeType() {
        final Category category = FileCategoryUtil.categorize("txt", "video/mp4");

        assertThat(category).isEqualTo(Category.DOCUMENT);
    }

    @Test
    @DisplayName("파일 카테고리 분류 : 낯선 확장자는 MIME Type으로 보정한다")
    void categorize_unknownExtension_fallsBackToMimeType() {
        assertThat(FileCategoryUtil.categorize("unknown", "video/mp4")).isEqualTo(Category.VIDEO);
        assertThat(FileCategoryUtil.categorize("unknown", "audio/mpeg")).isEqualTo(Category.AUDIO);
        assertThat(FileCategoryUtil.categorize("unknown", "text/plain; charset=UTF-8")).isEqualTo(Category.DOCUMENT);
        assertThat(FileCategoryUtil.categorize("", "application/pdf")).isEqualTo(Category.DOCUMENT);
    }

    @Test
    @DisplayName("파일 카테고리 분류 : SVG MIME Type은 IMAGE로 승격하지 않는다")
    void categorize_svgMimeType_returnsEtc() {
        final Category category = FileCategoryUtil.categorize("svg", "image/svg+xml");

        assertThat(category).isEqualTo(Category.ETC);
    }

    @Test
    @DisplayName("파일 카테고리 분류 : 확장자와 MIME Type이 모두 없으면 UNKNOWN, 확장자만 낯설면 ETC로 분류한다")
    void categorize_unknownInputs_returnsUnknownOrEtc() {
        assertThat(FileCategoryUtil.categorize(null, null)).isEqualTo(Category.UNKNOWN);
        assertThat(FileCategoryUtil.categorize("", "application/octet-stream")).isEqualTo(Category.UNKNOWN);
        assertThat(FileCategoryUtil.categorize("bin", "application/octet-stream")).isEqualTo(Category.ETC);
    }
}
