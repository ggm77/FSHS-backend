package org.iptime.raspinas.FSHS.exception.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ExceptionCode {
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "Email already exist."),


    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error");

    private final HttpStatus status;
    private final String message;
}
