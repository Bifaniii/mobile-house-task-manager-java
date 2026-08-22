package com.br.ms_usuario.exceptions;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário cadastrado com o email: " + email);
    }
}