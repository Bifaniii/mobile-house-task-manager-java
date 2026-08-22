package com.br.ms_usuario.domain.dto;

public record LoginResponseDTO(
        String token,
        String tipo,
        UsuarioResponseDTO usuario
) {
    public LoginResponseDTO(String token, UsuarioResponseDTO usuario) {
        this(token, "Bearer", usuario);
    }
}