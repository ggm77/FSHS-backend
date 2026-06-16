package com.seohamin.fshs.v2.global.util.storage;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.global.util.MimeTypeUtil;

import java.util.Locale;
import java.util.Map;

/**
 * 파일이 어떤 파일인지 구별하는 유틸 클래스
 * 항상 리턴 값은 Category.java Enum으로 보내기
 */
public final class FileCategoryUtil {

    private static final Map<String, Category> MIME_TYPE_MAP = Map.ofEntries(
            Map.entry("application/pdf", Category.DOCUMENT),
            Map.entry("application/msword", Category.DOCUMENT),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", Category.DOCUMENT),
            Map.entry("application/vnd.ms-powerpoint", Category.DOCUMENT),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", Category.DOCUMENT),
            Map.entry("application/vnd.ms-excel", Category.DOCUMENT),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Category.DOCUMENT),
            Map.entry("application/rtf", Category.DOCUMENT),
            Map.entry("application/vnd.oasis.opendocument.text", Category.DOCUMENT),
            Map.entry("application/vnd.oasis.opendocument.spreadsheet", Category.DOCUMENT),
            Map.entry("application/vnd.oasis.opendocument.presentation", Category.DOCUMENT),
            Map.entry("application/json", Category.DOCUMENT),
            Map.entry("application/xml", Category.DOCUMENT),
            Map.entry("application/yaml", Category.DOCUMENT),
            Map.entry("application/x-yaml", Category.DOCUMENT),
            Map.entry("image/svg+xml", Category.ETC),
            Map.entry("application/ogg", Category.AUDIO),
            Map.entry("application/zip", Category.ARCHIVE),
            Map.entry("application/x-7z-compressed", Category.ARCHIVE),
            Map.entry("application/vnd.rar", Category.ARCHIVE),
            Map.entry("application/x-rar-compressed", Category.ARCHIVE),
            Map.entry("application/x-tar", Category.ARCHIVE),
            Map.entry("application/gzip", Category.ARCHIVE),
            Map.entry("application/x-gzip", Category.ARCHIVE),
            Map.entry("application/x-bzip2", Category.ARCHIVE),
            Map.entry("application/x-xz", Category.ARCHIVE),
            Map.entry("application/zstd", Category.ARCHIVE),
            Map.entry("application/java-archive", Category.ARCHIVE)
    );

    // 인스턴스화 방지
    private FileCategoryUtil() {}

    /**
     * 파일 이름을 통해 파일의 카테고리를 가져오는 메서드
     * @param extension 파일의 확장자 (NFC, 소문자)
     * @return 분류된 카테고리 Enum
     */
    public static Category categorize(final String extension) {
        return categorize(extension, null);
    }

    /**
     * 파일 확장자와 MIME Type을 통해 파일의 카테고리를 가져오는 메서드
     * @param extension 파일의 확장자
     * @param mimeType MIME Type
     * @return 분류된 카테고리 Enum
     */
    public static Category categorize(
            final String extension,
            final String mimeType
    ) {
        final String normalizedExtension = normalizeExtension(extension);
        if (!normalizedExtension.isBlank()) {
            final Category category = Category.EXTENSION_MAP.get(normalizedExtension);
            if (category != null) {
                return category;
            }
        }

        final Category mimeCategory = categorizeMimeType(mimeType);
        if (mimeCategory != Category.UNKNOWN) {
            return mimeCategory;
        }

        return normalizedExtension.isBlank() ? Category.UNKNOWN : Category.ETC;
    }

    private static Category categorizeMimeType(final String mimeType) {
        final String normalizedMimeType = normalizeMimeType(mimeType);
        if (normalizedMimeType.isBlank()
                || MimeTypeUtil.DEFAULT_MIME_TYPE.equals(normalizedMimeType)) {
            return Category.UNKNOWN;
        }

        final Category exactCategory = MIME_TYPE_MAP.get(normalizedMimeType);
        if (exactCategory != null) {
            return exactCategory;
        }

        if (normalizedMimeType.startsWith("image/")) {
            return Category.IMAGE;
        }
        if (normalizedMimeType.startsWith("video/")) {
            return Category.VIDEO;
        }
        if (normalizedMimeType.startsWith("audio/")) {
            return Category.AUDIO;
        }
        if (normalizedMimeType.startsWith("text/")) {
            return Category.DOCUMENT;
        }

        return Category.UNKNOWN;
    }

    private static String normalizeExtension(final String extension) {
        if (extension == null || extension.isBlank()) {
            return "";
        }

        String normalized = extension.trim();
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeMimeType(final String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "";
        }

        final String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        final int parameterIndex = normalized.indexOf(';');
        if (parameterIndex == -1) {
            return normalized;
        }
        return normalized.substring(0, parameterIndex).trim();
    }
}
