package com.br.ms_tarefa.service;

import com.br.ms_tarefa.domain.Tarefa;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Escolhe quem faz a próxima tarefa de uma rotina. Olha as últimas duas semanas e dá a tarefa a quem
 * acumulou menos carga, medida em pontos (tarefa difícil vale mais). No empate, evita repetir a pessoa
 * da vez anterior; depois, prefere quem recebeu tarefa há mais tempo; por fim, segue a ordem da lista.
 */
public final class RodizioJusto {

    static final Duration JANELA = Duration.ofDays(14);

    private RodizioJusto() {
    }

    public static UUID escolher(List<UUID> participantes, List<Tarefa> historico, UUID ultimo, Instant agora) {
        Instant inicio = agora.minus(JANELA);
        List<Tarefa> recentes = historico.stream()
                .filter(t -> t.getCriadoEm() != null && t.getCriadoEm().toInstant().isAfter(inicio))
                .toList();

        return participantes.stream()
                .min(Comparator
                        .<UUID>comparingInt(p -> carga(p, recentes))
                        .thenComparing(p -> p.equals(ultimo))
                        .thenComparing(p -> ultimaTarefa(p, recentes))
                        .thenComparingInt(participantes::indexOf))
                .orElseThrow(() -> new IllegalArgumentException("A rotina não tem participantes"));
    }

    private static int carga(UUID pessoa, List<Tarefa> tarefas) {
        return tarefas.stream()
                .filter(t -> pessoa.equals(t.getUsuarioId()))
                .mapToInt(Tarefa::pontosOuPadrao)
                .sum();
    }

    private static Instant ultimaTarefa(UUID pessoa, List<Tarefa> tarefas) {
        return tarefas.stream()
                .filter(t -> pessoa.equals(t.getUsuarioId()))
                .map(t -> t.getCriadoEm().toInstant())
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH);
    }
}
