package com.br.ms_usuario.domain.dto;

import com.br.ms_usuario.domain.Parentesco;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(
        @NotBlank(message = "Nome é obrigatório") String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido") String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres") String password,

        @NotNull(message = "Parentesco é obrigatório") Parentesco parentesco
) {
}