package com.br.ms_usuario.exceptions;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends RuntimeException {
    public UsuarioNaoEncontradoException(UUID id) {
        super("Usuário não encontrado com id: " + id);
    }

    public UsuarioNaoEncontradoException(String email) {
        super("Usuário não encontrado com email: " + email);
    }
}