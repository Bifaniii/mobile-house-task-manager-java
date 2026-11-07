package com.br.ms_usuario.domain.dto;

import java.util.UUID;

/** O código de convite só é preenchido para mãe ou pai. */
public record FamiliaResponseDTO(UUID id, String nome, String codigoConvite) {
}
