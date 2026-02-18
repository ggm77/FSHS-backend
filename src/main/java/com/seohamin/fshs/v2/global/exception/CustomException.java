package com.seohamin.fshs.v2.global.exception;

import lombok.Getter;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;

@Getter
public class CustomException extends RuntimeException {

    private final ExceptionCode exceptionCode;

    public CustomException(final ExceptionCode exceptionCode) {
        super("");
        this.exceptionCode = exceptionCode;
    }

    public CustomException(final ExceptionCode exceptionCode, final Exception exception) {
        super(exception.getMessage(), exception);
        this.exceptionCode = exceptionCode;
    }
}
