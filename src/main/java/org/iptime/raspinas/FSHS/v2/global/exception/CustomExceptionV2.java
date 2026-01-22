package org.iptime.raspinas.FSHS.v2.global.exception;

import lombok.Getter;
import org.iptime.raspinas.FSHS.v2.global.exception.constants.ExceptionCodeV2;

@Getter
public class CustomExceptionV2 extends RuntimeException {

    private final ExceptionCodeV2 exceptionCodeV2;

    public CustomExceptionV2(final ExceptionCodeV2 exceptionCodeV2) {
        super("");
        this.exceptionCodeV2 = exceptionCodeV2;
    }
}
