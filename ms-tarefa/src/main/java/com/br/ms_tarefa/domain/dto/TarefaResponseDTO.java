package com.br.ms_tarefa.domain.dto;

import com.br.ms_tarefa.domain.Status;

import java.util.UUID;

public record TarefaResponseDTO(
        UUID id,
        String titulo,
        String descricao,
        UUID usuarioId,
        UUID criadoPor,
        Status status
) { }
