package org.iptime.raspinas.FSHS.exception.handler;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.exception.response.ExceptionResponse;
import org.iptime.raspinas.FSHS.tool.detector.DatabaseDownDetector;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;

@RestControllerAdvice
@RequiredArgsConstructor
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler{

    private final DatabaseDownDetector databaseDownDetector;

    @ExceptionHandler(CustomException.class)
    protected final ResponseEntity handleCustomExceptions(CustomException ex){
        if(ex.getExceptionCode().equals(ExceptionCode.DATABASE_DOWN)){
            databaseDownDetector.databaseDown();
        }
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), ex.getExceptionCode().getStatus(), ex.getExceptionCode().getMessage());
        return new ResponseEntity(exceptionResponse, ex.getExceptionCode().getStatus());
    }

    @ExceptionHandler({ Exception.class })
    protected final ResponseEntity handleAllExceptions(Exception ex){
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        ex.printStackTrace();
        return new ResponseEntity(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
