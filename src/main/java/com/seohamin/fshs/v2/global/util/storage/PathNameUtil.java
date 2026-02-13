package com.seohamin.fshs.v2.global.util.storage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;

/**
 * 파일이나 폴더의 이름에 대한 유틸 클래스
 */
public final class PathNameUtil {

    // 인스턴스화 방지
    private PathNameUtil() {}

    /**
     * 파일명이나 폴더명을 정규화하는 메서드
     * NFC로 변환함
     * @param path 확장자 포함한 파일명 또는 폴더명
     * @return 정규화 된 문자열
     */
    public static String normalize(final String path) {
        // 1) null 체크
        if (path == null || path.isBlank()) {
            return "";
        }

        // 2) NFC로 변환
        return Normalizer.normalize(path, Normalizer.Form.NFC);
    }

    /**
     * Path를 NFC로 변환하는 메서드
     * @param path 변환할 Path
     * @return 변환 된 Path
     */
    public static Path normalizePath(final Path path) {
        // 1) null 체크
        if (path == null) {
            return null;
        }

        // 2) 문자열로 변환
        final String pathStr = path.toString();

        // 3) NFC 변환
        return Path.of(normalize(pathStr));
    }

    /**
     * Path에서 파일명만 문자열로 추출하는 메서드
     * @param path Path 경로
     * @return 파일명 문자열
     */
    public static String extractFileNameFromPath(final Path path) {
        // 1) null 체크
        if (path == null) {
            return "";
        }

        // 2) 파일명 추출
        final Path fileName = path.getFileName();

        // 3) null 체크
        if (fileName == null) {
            return "";
        }

        return fileName.toString();
    }

    /**
     * 문자열 경로에서 파일명 추출하는 메서드
     * @param path 문자열로 된 경로
     * @return 파일명 문자열
     */
    public static String extractFileName(final String path) {
        // 1) null 체크
        if (path == null || path.isBlank()) {
            return "";
        }

        // 2) Path로 변환 후 추출
        return extractFileNameFromPath(Paths.get(path));
    }

    /**
     * 정규화 된 파일명에서 확장자 제외한 파일명만 추출하는 메서드
     * @param fileName 정규화 된 파일명
     * @return 확장자 없는 파일명
     */
    public static String extractBaseName(final String fileName) {
        // 1) null 체크
        if (fileName == null || fileName.isBlank()){
            return "";
        }

        // 2) 점 찾기
        final int dotIndex = fileName.lastIndexOf('.');

        // 3) 점이 맨 앞에 있거나 없는 경우 처리
        if (dotIndex <= 0) {
            return fileName;
        }

        // 4) 파일 이름 추출
        return fileName.substring(0, dotIndex);
    }

    /**
     * 정규화 된 파일명에서 확장자만 추출하는 메서드
     * @param fileName 정규화 된 파일명
     * @return 확장자
     */
    public static String extractExtension(final String fileName) {
        // 1) null 체크
        if (fileName == null || fileName.isBlank()){
            return "";
        }

        // 2) 확장자 존재 확인
        final int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1){
            return "";
        }

        return fileName.substring(dotIndex + 1);
    }
}
