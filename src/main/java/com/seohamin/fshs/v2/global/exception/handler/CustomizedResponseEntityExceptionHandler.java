package com.seohamin.fshs.v2.global.exception.handler;

import lombok.extern.slf4j.Slf4j;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.exception.response.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ExceptionResponse> handleCustomException(final CustomException ex) {

        if (ex.getCause() != null) {
            log.error("EXCEPTION CODE {}, message: {}", ex.getExceptionCode().name(), ex.getMessage(), ex.getCause());
        }

        final ExceptionResponse response = new ExceptionResponse(ex.getExceptionCode());

        return ResponseEntity.status(ex.getExceptionCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleAllException(final Exception ex) {
        log.error("INTERNAL SERVER ERROR: {}", ex.getMessage(), ex);

        final ExceptionResponse response = new ExceptionResponse(ExceptionCode.INTERNAL_SERVER_ERROR);

        return ResponseEntity.internalServerError().body(response);
    }
}
