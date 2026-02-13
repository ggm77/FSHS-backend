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

    IMAGE("이미지", List.of("jpg", "jpeg", "png", "gif", "webp", "heic")),
    VIDEO("동영상", List.of("mp4", "mkv", "mov", "avi", "wmv")),
    AUDIO("오디오", List.of("mp3", "wav", "flac", "ogg", "aac", "m4a")),
    DOCUMENT("문서", List.of("pdf", "txt", "doc", "docx", "ppt", "pptx", "xls", "xlsx")),
    ARCHIVE("압축파일", List.of("zip", "7z", "rar", "tar", "gz")),
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
