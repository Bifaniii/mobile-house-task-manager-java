package com.br.ms_tarefa.domain.dto;

import com.br.ms_tarefa.domain.Frequencia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RotinaRequestDTO(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 255, message = "Título deve ter no máximo 255 caracteres") String titulo,

        @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres") String descricao,

        @Min(value = 1, message = "A rotina vale pelo menos 1 ponto")
        @Max(value = 100, message = "A rotina vale no máximo 100 pontos") Integer pontos,

        Boolean exigeFoto,

        @NotNull(message = "Frequência é obrigatória") Frequencia frequencia,

        @NotEmpty(message = "Escolha pelo menos uma pessoa")
        @Size(max = 20, message = "Escolha no máximo 20 pessoas") List<UUID> participantes,

        /* Quando começa; sem valor, a primeira tarefa nasce agora. */
        Instant inicio,

        @Min(value = 1, message = "O prazo é de pelo menos 1 hora")
        @Max(value = 168, message = "O prazo é de no máximo 7 dias") Integer prazoHoras
) { }
