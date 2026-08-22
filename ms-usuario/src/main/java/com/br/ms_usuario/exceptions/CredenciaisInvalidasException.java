package com.br.ms_usuario.exceptions;

public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("Email ou senha inválidos");
    }
}