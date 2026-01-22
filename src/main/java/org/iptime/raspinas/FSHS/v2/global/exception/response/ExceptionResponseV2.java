package org.iptime.raspinas.FSHS.v2.global.exception.response;

import lombok.Getter;
import org.iptime.raspinas.FSHS.v2.global.exception.constants.ExceptionCodeV2;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class ExceptionResponseV2 {
    private final LocalDateTime timestamp;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public ExceptionResponseV2(final ExceptionCodeV2 exceptionCode) {
        this.timestamp = LocalDateTime.now();
        this.httpStatus = exceptionCode.getHttpStatus();
        this.code = exceptionCode.name();
        this.message = exceptionCode.getMessage();
    }
}
