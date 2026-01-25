package org.iptime.raspinas.FSHS.v2.global.exception;

import lombok.Getter;
import org.iptime.raspinas.FSHS.v2.global.exception.constants.ExceptionCode;

@Getter
public class CustomException extends RuntimeException {

    private final ExceptionCode exceptionCode;

    public CustomException(final ExceptionCode exceptionCode) {
        super("");
        this.exceptionCode = exceptionCode;
    }
}
