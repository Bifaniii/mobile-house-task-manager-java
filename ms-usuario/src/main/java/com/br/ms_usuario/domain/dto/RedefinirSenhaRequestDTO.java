package com.br.ms_usuario.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequestDTO(
        @NotBlank(message = "Token é obrigatório") String token,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres") String novaSenha
) {
}
