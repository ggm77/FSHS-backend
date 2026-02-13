package com.seohamin.fshs.v2.global.util;

import java.util.Map;

public class MimeTypeUtil {

    // 인스턴스화 방지
    private MimeTypeUtil() {}

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final Map<String, String> EXTENSION_MAP = Map.ofEntries(
            Map.entry("mp4", "video/mp4"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("txt", "text/plain"),
            Map.entry("zip", "application/zip"),
            Map.entry("json", "application/json")
    );

    public static String getMimeType(final String extension) {
        return EXTENSION_MAP.getOrDefault(extension, null);
    }
}
