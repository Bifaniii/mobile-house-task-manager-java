package com.br.ms_usuario.exceptions;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> detalhes
) {
    public ApiErrorResponse(int status, String error, String message) {
        this(Instant.now(), status, error, message, null);
    }

    public ApiErrorResponse(int status, String error, String message, List<String> detalhes) {
        this(Instant.now(), status, error, message, detalhes);
    }
}