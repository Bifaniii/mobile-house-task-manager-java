package com.br.ms_tarefa.domain.dto;

import com.br.ms_tarefa.domain.Frequencia;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RotinaResponseDTO(
        UUID id,
        String titulo,
        String descricao,
        int pontos,
        boolean exigeFoto,
        Frequencia frequencia,
        List<UUID> participantes,
        int prazoHoras,
        Instant proximaExecucao,
        boolean ativa,
        UUID proximoResponsavel
) { }
