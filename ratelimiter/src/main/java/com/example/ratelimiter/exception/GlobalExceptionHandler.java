package com.example.ratelimiter.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler{
    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<Map<String , String>> handleClientNotFound(
        ClientNotFoundException ex){

        Map<String , String> body = Map.of("error" , ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String , String>> handleBadRequest(IllegalArgumentException ex){
        Map<String , String> body = Map.of("error" , ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }
}