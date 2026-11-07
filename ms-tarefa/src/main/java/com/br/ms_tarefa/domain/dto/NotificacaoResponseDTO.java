package com.br.ms_tarefa.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificacaoResponseDTO(UUID id, String titulo, String mensagem, UUID tarefaId, boolean lida, Instant criadaEm) { }
