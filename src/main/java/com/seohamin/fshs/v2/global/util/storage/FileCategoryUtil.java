package com.seohamin.fshs.v2.global.util.storage;

import com.seohamin.fshs.v2.domain.file.entity.Category;

/**
 * 파일이 어떤 파일인지 구별하는 유틸 클래스
 * 항상 리턴 값은 Category.java Enum으로 보내기
 */
public final class FileCategoryUtil {

    // 인스턴스화 방지
    private FileCategoryUtil() {}

    /**
     * 파일 이름을 통해 파일의 카테고리를 가져오는 메서드
     * @param extension 파일의 확장자 (NFC, 소문자)
     * @return 분류된 카테고리 Enum
     */
    public static Category categorize(final String extension) {
        // 1) null 검사
        if(extension == null || extension.isBlank()) {
            return Category.UNKNOWN;
        }

        // 2) 확장자로 카테고리 가져오기 (해당하는 파일 없으면 카테고리가 ETC)
        return Category.EXTENSION_MAP.getOrDefault(extension, Category.ETC);
    }
}
