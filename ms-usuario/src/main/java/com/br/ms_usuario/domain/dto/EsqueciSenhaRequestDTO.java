package com.br.ms_usuario.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequestDTO(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido") String email
) {
}
