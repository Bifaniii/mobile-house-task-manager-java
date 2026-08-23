package com.br.ms_tarefa.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends ResponseStatusException {
    public UsuarioNaoEncontradoException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Usuário não encontrado com id: " + id);
    }
}