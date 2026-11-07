package com.br.ms_usuario.messaging;

import java.util.UUID;

public record UsuarioRemovidoEvent(UUID usuarioId, UUID familiaId) {
}
