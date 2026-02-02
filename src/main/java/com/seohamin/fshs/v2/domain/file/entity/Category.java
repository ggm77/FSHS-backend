package com.seohamin.fshs.v2.domain.file.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {

    IMAGE("이미지"),
    VIDEO("동영상"),
    AUDIO("오디오"),
    DOCUMENT("문서"),
    ARCHIVE("압축파일"),
    ETC("기타"), // 지원하지 않는 파일
    UNKNOWN("알 수 없음") // 분석 전이거나 실패한 파일
    ;

    private final String label;
}
