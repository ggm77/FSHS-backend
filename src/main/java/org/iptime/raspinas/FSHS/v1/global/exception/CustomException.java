package org.iptime.raspinas.FSHS.v1.global.exception;

import lombok.Getter;
import org.iptime.raspinas.FSHS.v1.global.exception.constants.ExceptionCode;

@Getter
public class CustomException extends RuntimeException{
    private final ExceptionCode exceptionCode;

    public CustomException(ExceptionCode exceptionCode){
        this.exceptionCode = exceptionCode;
    }
}
