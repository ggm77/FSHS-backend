package org.iptime.raspinas.FSHS.exception.handler;

import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.response.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;

@RestControllerAdvice
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler{

    @ExceptionHandler(CustomException.class)
    protected final ResponseEntity handleCustomExceptions(CustomException ex){
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), ex.getExceptionCode().getStatus(), ex.getExceptionCode().getMessage());
        return new ResponseEntity(exceptionResponse, ex.getExceptionCode().getStatus());
    }

    @ExceptionHandler({ Exception.class })
    protected final ResponseEntity handleAllExceptions(Exception ex){
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        return new ResponseEntity(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
