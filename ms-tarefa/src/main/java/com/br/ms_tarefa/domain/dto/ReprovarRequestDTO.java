package com.br.ms_tarefa.domain.dto;

import jakarta.validation.constraints.Size;

public record ReprovarRequestDTO(
        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres") String motivo
) { }
