package org.iptime.raspinas.FSHS.global.exception.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.global.exception.response.ExceptionResponse;
import org.iptime.raspinas.FSHS.global.exception.CustomException;
import org.iptime.raspinas.FSHS.global.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.global.util.detector.DatabaseDownDetector;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler{

    private final DatabaseDownDetector databaseDownDetector;

    @ExceptionHandler(CustomException.class)
    protected final ResponseEntity<?> handleCustomExceptions(final CustomException ex){

        //DB down exception | DB 다운 시에 예외 처리
        if(ex.getExceptionCode().equals(ExceptionCode.DATABASE_DOWN)){
            databaseDownDetector.databaseDown();
        }

        final ExceptionResponse exceptionResponse = new ExceptionResponse(
                new Date(),
                ex.getExceptionCode().getStatus(),
                ex.getExceptionCode().getMessage()
        );

        return new ResponseEntity(exceptionResponse, ex.getExceptionCode().getStatus());
    }

    @ExceptionHandler({ Exception.class })
    protected final ResponseEntity<?> handleAllExceptions(final Exception ex){
        final ExceptionResponse exceptionResponse = new ExceptionResponse(
                new Date(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );

        log.error("CustomizedResponseEntityExceptionHandler.handleAllExceptions message:{}",ex.getMessage(),ex);

        return new ResponseEntity(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
