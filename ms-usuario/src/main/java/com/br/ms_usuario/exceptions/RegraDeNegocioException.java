package com.br.ms_usuario.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RegraDeNegocioException extends RuntimeException {

    private final HttpStatus status;

    public RegraDeNegocioException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
}
