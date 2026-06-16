package com.seohamin.fshs.v2.global.util;

import java.util.Locale;
import java.util.Map;

public class MimeTypeUtil {

    // 인스턴스화 방지
    private MimeTypeUtil() {}

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final Map<String, String> EXTENSION_MAP = Map.ofEntries(
            Map.entry("mp4", "video/mp4"),
            Map.entry("m4v", "video/x-m4v"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("wmv", "video/x-ms-wmv"),
            Map.entry("webm", "video/webm"),
            Map.entry("flv", "video/x-flv"),
            Map.entry("mpg", "video/mpeg"),
            Map.entry("mpeg", "video/mpeg"),
            Map.entry("m2ts", "video/mp2t"),
            Map.entry("mts", "video/mp2t"),
            Map.entry("ts", "video/mp2t"),
            Map.entry("3gp", "video/3gpp"),
            Map.entry("3g2", "video/3gpp2"),
            Map.entry("ogv", "video/ogg"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("flac", "audio/flac"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("oga", "audio/ogg"),
            Map.entry("aac", "audio/aac"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("opus", "audio/opus"),
            Map.entry("wma", "audio/x-ms-wma"),
            Map.entry("aif", "audio/aiff"),
            Map.entry("aiff", "audio/aiff"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("heic", "image/heic"),
            Map.entry("heif", "image/heif"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("tif", "image/tiff"),
            Map.entry("tiff", "image/tiff"),
            Map.entry("ico", "image/vnd.microsoft.icon"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("txt", "text/plain"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("csv", "text/csv"),
            Map.entry("tsv", "text/tab-separated-values"),
            Map.entry("md", "text/markdown"),
            Map.entry("rtf", "application/rtf"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
            Map.entry("odp", "application/vnd.oasis.opendocument.presentation"),
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("xml", "application/xml"),
            Map.entry("json", "application/json"),
            Map.entry("yaml", "application/yaml"),
            Map.entry("yml", "application/yaml"),
            Map.entry("log", "text/plain"),
            Map.entry("zip", "application/zip"),
            Map.entry("7z", "application/x-7z-compressed"),
            Map.entry("rar", "application/vnd.rar"),
            Map.entry("tar", "application/x-tar"),
            Map.entry("gz", "application/gzip"),
            Map.entry("tgz", "application/gzip"),
            Map.entry("bz2", "application/x-bzip2"),
            Map.entry("tbz2", "application/x-bzip2"),
            Map.entry("xz", "application/x-xz"),
            Map.entry("txz", "application/x-xz"),
            Map.entry("zst", "application/zstd"),
            Map.entry("jar", "application/java-archive"),
            Map.entry("war", "application/java-archive")
    );

    public static String getMimeType(final String extension) {
        return EXTENSION_MAP.getOrDefault(normalizeExtension(extension), null);
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
}
