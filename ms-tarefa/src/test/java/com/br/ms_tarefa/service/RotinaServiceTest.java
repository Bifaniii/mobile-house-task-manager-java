package com.br.ms_tarefa.service;

import com.br.ms_tarefa.client.UsuarioClient;
import com.br.ms_tarefa.domain.Frequencia;
import com.br.ms_tarefa.domain.Parentesco;
import com.br.ms_tarefa.domain.Rotina;
import com.br.ms_tarefa.domain.Tarefa;
import com.br.ms_tarefa.domain.dto.RotinaRequestDTO;
import com.br.ms_tarefa.domain.dto.UsuarioResponseDTO;
import com.br.ms_tarefa.exceptions.UsuarioNaoEncontradoException;
import com.br.ms_tarefa.repository.RotinaRepository;
import com.br.ms_tarefa.repository.TarefaRepository;
import com.br.ms_tarefa.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RotinaServiceTest {

    private static final String AUTH = "Bearer token";

    @Mock
    private RotinaRepository repository;
    @Mock
    private TarefaRepository tarefaRepository;
    @Mock
    private NotificacaoService notificacaoService;
    @Mock
    private UsuarioClient usuarioClient;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RotinaService service;

    private final UUID familia = UUID.randomUUID();
    private final UUID mae = UUID.randomUUID();
    private final UUID bia = UUID.randomUUID();
    private final UUID leo = UUID.randomUUID();

    private Rotina rotinaDevida(Instant quando) {
        Rotina rotina = new Rotina();
        rotina.setId(UUID.randomUUID());
        rotina.setFamiliaId(familia);
        rotina.setTitulo("Tirar o lixo");
        rotina.setPontos(3);
        rotina.setExigeFoto(true);
        rotina.setFrequencia(Frequencia.DIARIA);
        rotina.setParticipantes(new ArrayList<>(List.of(bia, leo)));
        rotina.setPrazoHoras(6);
        rotina.setProximaExecucao(quando);
        rotina.setCriadoPor(mae);
        return rotina;
    }

    private Tarefa gerarUmaTarefa(Rotina rotina) {
        when(repository.findByFamiliaIdAndAtivaTrueAndProximaExecucaoLessThanEqual(eq(familia), any())).thenReturn(List.of(rotina));
        when(tarefaRepository.findByFamiliaId(familia)).thenReturn(List.of());
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> {
            Tarefa t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        service.gerarDevidas(familia);

        ArgumentCaptor<Tarefa> criada = ArgumentCaptor.forClass(Tarefa.class);
        verify(tarefaRepository).save(criada.capture());
        return criada.getValue();
    }

    @Test
    void gerarDevidas_criaTarefaHerdandoARotinaComPrazoEAvisa() {
        Rotina rotina = rotinaDevida(Instant.now().minusSeconds(10));

        Tarefa tarefa = gerarUmaTarefa(rotina);

        assertThat(tarefa.getRotinaId()).isEqualTo(rotina.getId());
        assertThat(tarefa.getFamiliaId()).isEqualTo(familia);
        assertThat(tarefa.getPontos()).isEqualTo(3);
        assertThat(tarefa.getExigeFoto()).isTrue();
        assertThat(tarefa.getCriadoPor()).isEqualTo(mae);
        assertThat(tarefa.getPrazo()).isBetween(Instant.now().plus(Duration.ofHours(5)), Instant.now().plus(Duration.ofHours(7)));
        assertThat(rotina.getUltimoResponsavel()).isEqualTo(tarefa.getUsuarioId());
        verify(notificacaoService).notificar(eq(tarefa.getUsuarioId()), any(), any(), eq(tarefa.getId()));
    }

    @Test
    void gerarDevidas_depoisDeDiasParadoGeraUmaSoEPulaParaOFuturo() {
        Instant cincoDiasAtras = Instant.now().minus(Duration.ofDays(5));
        Rotina rotina = rotinaDevida(cincoDiasAtras);

        gerarUmaTarefa(rotina);

        assertThat(rotina.getProximaExecucao()).isAfter(Instant.now());
        verify(tarefaRepository, times(1)).save(any());
    }

    @Test
    void gerarDevidas_rotinaSemParticipantesEDesativadaSemCriarTarefa() {
        Rotina rotina = rotinaDevida(Instant.now().minusSeconds(10));
        rotina.setParticipantes(new ArrayList<>());
        when(repository.findByFamiliaIdAndAtivaTrueAndProximaExecucaoLessThanEqual(eq(familia), any())).thenReturn(List.of(rotina));

        service.gerarDevidas(familia);

        assertThat(rotina.isAtiva()).isFalse();
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void removerParticipante_tiraDaListaEDesativaSeFicarVazia() {
        Rotina duas = rotinaDevida(Instant.now());
        Rotina soDela = rotinaDevida(Instant.now());
        soDela.setParticipantes(new ArrayList<>(List.of(bia)));
        when(repository.findAll()).thenReturn(List.of(duas, soDela));

        service.removerParticipante(bia);

        assertThat(duas.getParticipantes()).containsExactly(leo);
        assertThat(duas.isAtiva()).isTrue();
        assertThat(soDela.getParticipantes()).isEmpty();
        assertThat(soDela.isAtiva()).isFalse();
    }

    @Test
    void criar_recusaParticipanteDeOutraFamilia() {
        UUID deFora = UUID.randomUUID();
        when(jwtService.autenticado(AUTH)).thenReturn(mae);
        when(usuarioClient.buscarUsuario(mae, AUTH)).thenReturn(new UsuarioResponseDTO(mae, "Ana", "a@c.com", Parentesco.MAE));
        when(usuarioClient.buscarUsuario(deFora, AUTH)).thenThrow(new UsuarioNaoEncontradoException(deFora));
        RotinaRequestDTO dto = new RotinaRequestDTO("Lixo", null, 1, false, Frequencia.DIARIA, List.of(deFora), null, 24);

        assertThatThrownBy(() -> service.criar(dto, AUTH)).isInstanceOf(UsuarioNaoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void frequencia_pulaOsPeriodosPerdidos() {
        Instant agora = Instant.parse("2026-11-10T12:00:00Z");

        assertThat(Frequencia.SEMANAL.proximaDepoisDe(Instant.parse("2026-10-01T08:00:00Z"), agora))
                .isEqualTo(Instant.parse("2026-11-12T08:00:00Z"));
    }
}
