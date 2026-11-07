package com.br.ms_tarefa.service;

import com.br.ms_tarefa.domain.Tarefa;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RodizioJustoTest {

    private final Instant agora = Instant.parse("2026-11-07T12:00:00Z");
    private final UUID ana = UUID.randomUUID();
    private final UUID bia = UUID.randomUUID();
    private final List<UUID> ambos = List.of(ana, bia);

    private Tarefa feita(UUID pessoa, int pontos, Duration haQuanto) {
        Tarefa tarefa = new Tarefa();
        tarefa.setUsuarioId(pessoa);
        tarefa.setPontos(pontos);
        tarefa.setCriadoEm(agora.minus(haQuanto).atZone(ZoneOffset.UTC));
        return tarefa;
    }

    @Test
    void daATarefaAQuemTemMenosCargaMedidaEmPontos() {
        // Bia fez duas tarefas fáceis; Ana fez uma difícil que pesa mais.
        List<Tarefa> historico = List.of(
                feita(ana, 5, Duration.ofDays(1)),
                feita(bia, 1, Duration.ofDays(2)),
                feita(bia, 1, Duration.ofDays(3)));

        assertThat(RodizioJusto.escolher(ambos, historico, null, agora)).isEqualTo(bia);
    }

    @Test
    void noEmpateEvitaRepetirQuemFezPorUltimo() {
        List<Tarefa> historico = List.of(feita(ana, 2, Duration.ofDays(1)), feita(bia, 2, Duration.ofDays(2)));

        assertThat(RodizioJusto.escolher(ambos, historico, ana, agora)).isEqualTo(bia);
        assertThat(RodizioJusto.escolher(ambos, historico, bia, agora)).isEqualTo(ana);
    }

    @Test
    void noEmpateSemUltimoPrefereQuemRecebeuHaMaisTempo() {
        List<Tarefa> historico = List.of(feita(ana, 2, Duration.ofDays(1)), feita(bia, 2, Duration.ofDays(5)));

        assertThat(RodizioJusto.escolher(ambos, historico, null, agora)).isEqualTo(bia);
    }

    @Test
    void tarefasForaDaJanelaDeDuasSemanasNaoContam() {
        List<Tarefa> historico = List.of(feita(ana, 50, Duration.ofDays(30)), feita(bia, 1, Duration.ofDays(1)));

        assertThat(RodizioJusto.escolher(ambos, historico, null, agora)).isEqualTo(ana);
    }

    @Test
    void semHistoricoSegueAOrdemDaLista() {
        assertThat(RodizioJusto.escolher(ambos, List.of(), null, agora)).isEqualTo(ana);
    }

    @Test
    void aoLongoDoTempoACargaFicaEquilibrada() {
        List<Tarefa> historico = new ArrayList<>();
        UUID ultimo = null;
        for (int dia = 13; dia >= 0; dia--) {
            Instant quando = agora.minus(Duration.ofDays(dia));
            UUID escolhida = RodizioJusto.escolher(ambos, historico, ultimo, quando);
            Tarefa tarefa = new Tarefa();
            tarefa.setUsuarioId(escolhida);
            tarefa.setPontos(2);
            tarefa.setCriadoEm(quando.atZone(ZoneOffset.UTC));
            historico.add(tarefa);
            ultimo = escolhida;
        }

        assertThat(historico.stream().filter(t -> t.getUsuarioId().equals(ana)).count()).isEqualTo(7);
        assertThat(historico.stream().filter(t -> t.getUsuarioId().equals(bia)).count()).isEqualTo(7);
    }
}
