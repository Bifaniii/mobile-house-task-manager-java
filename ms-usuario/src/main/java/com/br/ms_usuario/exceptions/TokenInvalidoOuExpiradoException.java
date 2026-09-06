package com.br.ms_usuario.exceptions;

public class TokenInvalidoOuExpiradoException extends RuntimeException {
    public TokenInvalidoOuExpiradoException() {
        super("Token de redefinição de senha inválido, expirado ou já utilizado");
    }
}
