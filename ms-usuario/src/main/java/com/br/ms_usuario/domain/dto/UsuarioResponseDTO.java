package com.br.ms_usuario.domain.dto;

import com.br.ms_usuario.domain.Parentesco;

import java.util.UUID;

public record UsuarioResponseDTO(
        UUID id,
        String nome,
        String email,
        Parentesco parentesco
) {
}
