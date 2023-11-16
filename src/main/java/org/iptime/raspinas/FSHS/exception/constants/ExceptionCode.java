package org.iptime.raspinas.FSHS.exception.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ExceptionCode {
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "Email already exist."),
    USER_EMAIL_NOT_EXIST(HttpStatus.NOT_ACCEPTABLE, "User email not exist."),
    USER_ID_NOT_EXIST(HttpStatus.NOT_ACCEPTABLE, "User not exist."),
    FILE_ID_NOT_EXIST(HttpStatus.NOT_ACCEPTABLE, "File not exist."),
    PASSWORD_NOT_MATCHED(HttpStatus.FORBIDDEN, "Password not correct."),
    TOKEN_AND_ID_NOT_MATCHED(HttpStatus.UNAUTHORIZED, "Token and id not matched."),
    FILE_NOT_UPLOADED(HttpStatus.BAD_REQUEST, "No files were uploaded."),
    FILE_ACCESS_DENY(HttpStatus.FORBIDDEN, "Cannot access other people's files."),
    PATH_NOT_VALID(HttpStatus.NOT_ACCEPTABLE, "Path is not valid; it must start with a '/' and end with a '/'."),


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
