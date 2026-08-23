package com.br.ms_tarefa.domain.dto;

import com.br.ms_tarefa.domain.Parentesco;

import java.util.UUID;


public record UsuarioResponseDTO(
        UUID id,
        String nome,
        String email,
        Parentesco parentesco
) {
}
