package com.mahdi.url_shortener.exception;

public class InvalidExpirationTimeException extends RuntimeException {
    public InvalidExpirationTimeException() {
        super("Expiration time must be in the future");
    }
    
}
