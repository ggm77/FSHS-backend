package org.iptime.raspinas.FSHS.exception.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ExceptionCode {
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "Email already exist."),
    USER_NOT_EXIST(HttpStatus.NOT_ACCEPTABLE, "User email not exist."),
    PASSWORD_NOT_MATCHED(HttpStatus.FORBIDDEN, "Password not correct."),


    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error");

    private final HttpStatus status;
    private final String message;
}
