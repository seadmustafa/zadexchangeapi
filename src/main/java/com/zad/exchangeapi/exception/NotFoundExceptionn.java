package com.example.balanceapi.exception;

/**
 * Custom exception for resource-not-found cases (e.g. missing User or Account).
 */
public class NotFoundExceptionn extends RuntimeException {

    public NotFoundExceptionn(String message) {
        super(message);
    }
}
