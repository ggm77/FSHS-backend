package org.iptime.raspinas.FSHS.v2.global.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.v2.global.exception.CustomExceptionV2;
import org.iptime.raspinas.FSHS.v2.global.exception.constants.ExceptionCodeV2;
import org.iptime.raspinas.FSHS.v2.global.exception.response.ExceptionResponseV2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class CustomizedResponseEntityExceptionHandlerV2 extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomExceptionV2.class)
    public ResponseEntity<ExceptionResponseV2> handleCustomException(final CustomExceptionV2 ex) {

        final ExceptionResponseV2 response = new ExceptionResponseV2(ex.getExceptionCodeV2());

        return ResponseEntity.status(ex.getExceptionCodeV2().getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponseV2> handleAllException(final Exception ex) {
        log.error("INTERNAL SERVER ERROR: {}", ex.getMessage(), ex);

        final ExceptionResponseV2 response = new ExceptionResponseV2(ExceptionCodeV2.INTERNAL_SERVER_ERROR);

        return ResponseEntity.internalServerError().body(response);
    }
}
