package org.iptime.raspinas.FSHS.common.exception;

import lombok.Getter;
import org.iptime.raspinas.FSHS.common.exception.constants.ExceptionCode;

@Getter
public class CustomException extends RuntimeException{
    private final ExceptionCode exceptionCode;

    public CustomException(ExceptionCode exceptionCode){
        this.exceptionCode = exceptionCode;
    }
}
