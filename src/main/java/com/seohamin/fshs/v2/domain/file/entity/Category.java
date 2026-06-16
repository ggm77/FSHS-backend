package com.seohamin.fshs.v2.domain.file.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum Category {

    IMAGE("이미지", List.of("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "tif", "tiff", "ico")),
    VIDEO("동영상", List.of("mp4", "m4v", "mkv", "mov", "avi", "wmv", "webm", "flv", "mpg", "mpeg", "m2ts", "mts", "ts", "3gp", "3g2", "ogv")),
    AUDIO("오디오", List.of("mp3", "wav", "flac", "ogg", "oga", "aac", "m4a", "opus", "wma", "aif", "aiff")),
    DOCUMENT("문서", List.of("pdf", "txt", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "csv", "tsv", "md", "rtf", "odt", "ods", "odp", "html", "htm", "xml", "json", "yaml", "yml", "log")),
    ARCHIVE("압축파일", List.of("zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "tbz2", "xz", "txz", "zst", "jar", "war")),
    ETC("기타", Collections.emptyList()),
    UNKNOWN("알 수 없음", Collections.emptyList()) // 분류가 안되었거나 실패한 파일
    ;

    private final String label;
    private final List<String> extensions;

    // 역방향 조회를 위한 맵
    public static final Map<String, Category> EXTENSION_MAP = Stream.of(values())
            .flatMap(category -> category.extensions.stream()
                    .map(ext -> Map.entry(ext.toLowerCase(), category)))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
}
