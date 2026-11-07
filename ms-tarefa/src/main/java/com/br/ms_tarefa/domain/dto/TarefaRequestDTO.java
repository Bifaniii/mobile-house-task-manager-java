package com.br.ms_tarefa.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

// domain/dto/TarefaRequestDTO.java
public record TarefaRequestDTO(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 255, message = "Título deve ter no máximo 255 caracteres") String titulo,

        @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres") String descricao,

        @NotNull(message = "Responsável é obrigatório") UUID usuarioId,

        Instant prazo,

        @Min(value = 1, message = "A tarefa vale pelo menos 1 ponto")
        @Max(value = 100, message = "A tarefa vale no máximo 100 pontos") Integer pontos,

        Boolean exigeFoto
) { }
