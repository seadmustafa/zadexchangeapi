package com.zad.exchangeapi.dto.response;

public record GenericResponse(
        boolean success,
        String message
) {
}