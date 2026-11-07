package com.br.ms_usuario.domain.dto;

import com.br.ms_usuario.domain.Parentesco;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cadastro público de mãe ou pai: informe nomeFamilia para criar uma família ou codigoConvite para entrar em uma. */
public record UsuarioRequestDTO(
        @NotBlank(message = "Nome é obrigatório") String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido") String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres") String password,

        @NotNull(message = "Parentesco é obrigatório") Parentesco parentesco,

        @Size(max = 80, message = "Nome da família deve ter no máximo 80 caracteres") String nomeFamilia,

        String codigoConvite
) {
}
