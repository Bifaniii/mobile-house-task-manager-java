package com.br.ms_usuario.messaging;

import java.io.Serializable;
import java.util.UUID;

public record UsuarioCriadoEvent(
        UUID id,
        String nome,
        String email
) implements Serializable {
}