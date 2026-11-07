package com.br.ms_tarefa.domain;

import java.time.Duration;
import java.time.Instant;

public enum Frequencia {
    DIARIA(Duration.ofDays(1)),
    SEMANAL(Duration.ofDays(7));

    private final Duration intervalo;

    Frequencia(Duration intervalo) {
        this.intervalo = intervalo;
    }

    /** Se o serviço ficou parado, pula o que passou: nasce uma tarefa só, não uma pilha delas. */
    public Instant proximaDepoisDe(Instant referencia, Instant agora) {
        Instant proxima = referencia.plus(intervalo);
        while (!proxima.isAfter(agora)) {
            proxima = proxima.plus(intervalo);
        }
        return proxima;
    }
}
