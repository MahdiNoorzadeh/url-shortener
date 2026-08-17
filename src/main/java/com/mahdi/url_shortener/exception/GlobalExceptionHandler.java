package com.mahdi.url_shortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUrlNotFound(
            UrlNotFoundException exception
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "URL_NOT_FOUND",
                exception.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

     @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message,
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleInvalidJson(
        HttpMessageNotReadableException exception
        ) {
    ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_REQUEST",
            "Malformed JSON request",
            LocalDateTime.now()
    );

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
        }

        @ExceptionHandler(UrlExpiredException.class)
        public ResponseEntity<ErrorResponse> handleUrlExpired(
        UrlExpiredException exception
)       {
    ErrorResponse response = new ErrorResponse(
        410,
        "URL_EXPIRED",
        exception.getMessage(),
        LocalDateTime.now()
        );

    return ResponseEntity
        .status(HttpStatus.GONE)
        .body(response);
        }

        @ExceptionHandler(InvalidExpirationTimeException.class)
        public ResponseEntity<ErrorResponse> handleInvalidExpirationTime(
        InvalidExpirationTimeException ex
        ) {
        ErrorResponse error = new ErrorResponse(
        400,
        "INVALID_EXPIRATION_TIME",
        ex.getMessage(),
        LocalDateTime.now()
        );

        return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(error);
        }
}


