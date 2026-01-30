package com.exceptionHandler;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(Long orderId) {
        super("Access denied to order with id = " + orderId);
    }
}
