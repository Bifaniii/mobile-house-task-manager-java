package com.br.ms_tarefa.domain.dto;

import com.br.ms_tarefa.domain.Status;

import java.time.Instant;
import java.util.UUID;

public record TarefaResponseDTO(
        UUID id,
        String titulo,
        String descricao,
        UUID usuarioId,
        UUID criadoPor,
        Status status,
        Instant prazo,
        int pontos,
        boolean exigeFoto,
        String motivoReprovacao,
        UUID rotinaId
) { }
