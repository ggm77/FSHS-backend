package org.iptime.raspinas.FSHS.exception.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ExceptionCode {
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "Email already exist."),
    USER_EMAIL_NOT_EXIST(HttpStatus.BAD_REQUEST, "User email not exist."),
    USER_ID_NOT_EXIST(HttpStatus.BAD_REQUEST, "User not exist."),
    FILE_ID_NOT_EXIST(HttpStatus.BAD_REQUEST, "File not exist."),
    PASSWORD_NOT_MATCHED(HttpStatus.FORBIDDEN, "Password not correct."),
    TOKEN_AND_ID_NOT_MATCHED(HttpStatus.UNAUTHORIZED, "Token and id not matched."),
    FILE_NOT_UPLOADED(HttpStatus.BAD_REQUEST, "No files were uploaded."),
    FILE_ACCESS_DENY(HttpStatus.FORBIDDEN, "Cannot access other people's files."),
    PATH_NOT_VALID(HttpStatus.BAD_REQUEST, "Path is not valid."),
    FILE_NOT_EXIST(HttpStatus.BAD_REQUEST, "File not exist."),
    TOKEN_NOT_VALID(HttpStatus.UNAUTHORIZED, "Token is not valid."),
    DIR_NOT_EXIST(HttpStatus.BAD_REQUEST, "Directory is not exist."),
    DIR_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "Directory is already exist."),


    FAILED_TO_CONVERT_SVG_TO_JPEG(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to convert svg file to jpeg."),
    FAILED_TO_GET_MEDIA_INFO(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get info from media."),
    FAILED_TO_GENERATE_THUMBNAIL(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate thumbnail from uploaded file."),
    FAILED_TO_CREATE_THUMBNAIL(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create thumbnail."),
    FILE_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "File is missing."),
    FAILED_TO_SAVE_FILE_IN_DIR(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save the file in server."),
    FAILED_TO_MAKE_DIR(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to make directory."),
    DATABASE_DOWN(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE DOWN"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error");

    private final HttpStatus status;
    private final String message;
}
