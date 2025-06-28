package com.zad.exchangeapi.exception;

import com.zad.exchangeapi.dto.response.GenericResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler using @RestControllerAdvice
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(com.example.balanceapi.exception.NotFoundExceptionn.class)
    public ResponseEntity<GenericResponse> handleNotFound(com.example.balanceapi.exception.NotFoundExceptionn ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse(false, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse(false, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                (FieldError fieldError) -> errors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse(false, "Internal server error: " + ex.getMessage()));
    }
}
