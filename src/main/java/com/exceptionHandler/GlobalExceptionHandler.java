package com.exceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleOrderNotFound(OrderNotFoundException ex) {
        return Map.of(
                "error", "ORDER_NOT_FOUND",
                "message", ex.getMessage()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleAccessDenied(AccessDeniedException ex) {
        return Map.of(
                "error", "ACCESS_DENIED",
                "message", ex.getMessage()
        );
    }

    // Универсальный fallback для всех остальных исключений
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleOtherExceptions(Exception ex) {
        return Map.of(
                "error", "INTERNAL_ERROR",
                "message", ex.getMessage()
        );
    }

}
