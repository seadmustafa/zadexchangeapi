package com.zad.exchangeapi.exception;


import org.springframework.http.HttpStatus;

public class ExchangeApiException extends RuntimeException {
    private final HttpStatus status;

    public ExchangeApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ExchangeApiException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR); // default fallback
    }

    public HttpStatus getStatus() {
        return status;
    }
}
