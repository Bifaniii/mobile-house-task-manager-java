package com.br.ms_tarefa.service;

import com.br.ms_tarefa.client.UsuarioClient;
import com.br.ms_tarefa.domain.FotoTarefa;
import com.br.ms_tarefa.domain.Parentesco;
import com.br.ms_tarefa.domain.Status;
import com.br.ms_tarefa.domain.Tarefa;
import com.br.ms_tarefa.domain.dto.TarefaRequestDTO;
import com.br.ms_tarefa.domain.dto.TarefaResponseDTO;
import com.br.ms_tarefa.domain.dto.UsuarioResponseDTO;
import com.br.ms_tarefa.exceptions.UsuarioNaoEncontradoException;
import com.br.ms_tarefa.repository.FotoTarefaRepository;
import com.br.ms_tarefa.repository.NotificacaoRepository;
import com.br.ms_tarefa.repository.TarefaRepository;
import com.br.ms_tarefa.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    private static final String AUTH = "Bearer token";

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private JwtService jwtService;

    @Mock
    private NotificacaoService notificacaoService;

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RotinaService rotinaService;

    @Mock
    private FotoTarefaRepository fotoRepository;

    @InjectMocks
    private TarefaService service;

    private final UUID maeId = UUID.randomUUID();
    private final UUID filhoId = UUID.randomUUID();
    private final UUID familiaId = UUID.randomUUID();

    @BeforeEach
    void familiaDoToken() {
        lenient().when(jwtService.familiaDe(AUTH)).thenReturn(familiaId);
    }

    private void logadoComo(UUID id, Parentesco parentesco) {
        when(jwtService.autenticado(AUTH)).thenReturn(id);
        lenient().when(usuarioClient.buscarUsuario(id, AUTH))
                .thenReturn(new UsuarioResponseDTO(id, "Usuário", "u@casa.com", parentesco));
    }

    private Tarefa tarefa(Status status) {
        Tarefa tarefa = new Tarefa();
        tarefa.setId(UUID.randomUUID());
        tarefa.setTitulo("Lavar a louça");
        tarefa.setUsuarioId(filhoId);
        tarefa.setCriadoPor(maeId);
        tarefa.setStatus(status);
        tarefa.setFamiliaId(familiaId);
        lenient().when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        lenient().when(tarefaRepository.save(tarefa)).thenReturn(tarefa);
        return tarefa;
    }

    private static TarefaRequestDTO pedido(String titulo, UUID responsavel) {
        return new TarefaRequestDTO(titulo, null, responsavel, null, null, null);
    }

    private static void assertStatus(Runnable acao, HttpStatus esperado) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(esperado);
    }

    @Test
    void criar_maeDeveCriarTarefaPendenteParaOFilho() {
        logadoComo(maeId, Parentesco.MAE);
        when(usuarioClient.buscarUsuario(filhoId, AUTH))
                .thenReturn(new UsuarioResponseDTO(filhoId, "Pedro", "pedro@casa.com", Parentesco.FILHO));
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

        TarefaResponseDTO criada = service.criar(pedido("Lavar a louça", filhoId), AUTH);

        assertThat(criada.status()).isEqualTo(Status.PENDENTE);
        assertThat(criada.usuarioId()).isEqualTo(filhoId);
        assertThat(criada.criadoPor()).isEqualTo(maeId);
        assertThat(criada.pontos()).isEqualTo(1);

        ArgumentCaptor<Tarefa> salva = ArgumentCaptor.forClass(Tarefa.class);
        verify(tarefaRepository).save(salva.capture());
        assertThat(salva.getValue().getFamiliaId()).isEqualTo(familiaId);
        verify(notificacaoService).notificar(eq(filhoId), any(), any(), any());
    }

    @Test
    void criar_prazoNoPassadoRecebe400() {
        logadoComo(maeId, Parentesco.MAE);
        TarefaRequestDTO dto = new TarefaRequestDTO("Lavar", null, filhoId, Instant.now().minusSeconds(60), 2, false);

        assertStatus(() -> service.criar(dto, AUTH), HttpStatus.BAD_REQUEST);
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void criar_filhoNaoPodeCriarTarefa() {
        logadoComo(filhoId, Parentesco.FILHO);

        assertStatus(() -> service.criar(pedido("Jogar videogame", filhoId), AUTH), HttpStatus.FORBIDDEN);
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void criar_deveFalharQuandoResponsavelNaoExisteNoMsUsuario() {
        logadoComo(maeId, Parentesco.MAE);
        UUID inexistente = UUID.randomUUID();
        when(usuarioClient.buscarUsuario(inexistente, AUTH)).thenThrow(new UsuarioNaoEncontradoException(inexistente));

        assertStatus(() -> service.criar(pedido("Lavar a louça", inexistente), AUTH), HttpStatus.NOT_FOUND);
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void listar_geraRotinasDevidasAntesDeBuscarSoDaFamilia() {
        when(jwtService.autenticado(AUTH)).thenReturn(maeId);
        when(tarefaRepository.findByFamiliaId(familiaId)).thenReturn(List.of());

        service.listarTarefa(AUTH);

        var ordem = inOrder(rotinaService, tarefaRepository);
        ordem.verify(rotinaService).gerarDevidas(familiaId);
        ordem.verify(tarefaRepository).findByFamiliaId(familiaId);
    }

    @Test
    void buscarPorId_tarefaDeOutraFamiliaRecebe404() {
        Tarefa tarefa = tarefa(Status.PENDENTE);
        tarefa.setFamiliaId(UUID.randomUUID());
        when(jwtService.autenticado(AUTH)).thenReturn(maeId);

        assertStatus(() -> service.buscarPorId(tarefa.getId(), AUTH), HttpStatus.NOT_FOUND);
    }

    @Test
    void iniciar_responsavelMoveDePendenteParaEmAndamento() {
        Tarefa tarefa = tarefa(Status.PENDENTE);
        when(jwtService.autenticado(AUTH)).thenReturn(filhoId);

        assertThat(service.iniciar(tarefa.getId(), AUTH).status()).isEqualTo(Status.EM_ANDAMENTO);
    }

    @Test
    void iniciar_quemNaoEResponsavelRecebe403() {
        Tarefa tarefa = tarefa(Status.PENDENTE);
        when(jwtService.autenticado(AUTH)).thenReturn(UUID.randomUUID());

        assertStatus(() -> service.iniciar(tarefa.getId(), AUTH), HttpStatus.FORBIDDEN);
    }

    @Test
    void iniciar_tarefaQueNaoEstaPendenteRecebe409() {
        Tarefa tarefa = tarefa(Status.EM_ANDAMENTO);
        when(jwtService.autenticado(AUTH)).thenReturn(filhoId);

        assertStatus(() -> service.iniciar(tarefa.getId(), AUTH), HttpStatus.CONFLICT);
    }

    @Test
    void concluir_vaiParaAguardandoAprovacaoENaoDiretoParaConcluida() {
        Tarefa tarefa = tarefa(Status.EM_ANDAMENTO);
        when(jwtService.autenticado(AUTH)).thenReturn(filhoId);

        assertThat(service.concluir(tarefa.getId(), AUTH).status()).isEqualTo(Status.AGUARDANDO_APROVACAO);
        verify(notificacaoService).notificar(eq(maeId), any(), any(), any());
    }

    @Test
    void concluir_tarefaJaConcluidaRecebe409() {
        Tarefa tarefa = tarefa(Status.CONCLUIDA);
        when(jwtService.autenticado(AUTH)).thenReturn(filhoId);

        assertStatus(() -> service.concluir(tarefa.getId(), AUTH), HttpStatus.CONFLICT);
    }

    @Test
    void concluir_semFotoDoDepoisQuandoExigidaRecebe409() {
        Tarefa tarefa = tarefa(Status.EM_ANDAMENTO);
        tarefa.setExigeFoto(true);
        when(jwtService.autenticado(AUTH)).thenReturn(filhoId);
        when(fotoRepository.existsByTarefaIdAndTipo(tarefa.getId(), FotoTarefa.Tipo.DEPOIS)).thenReturn(false);

        assertStatus(() -> service.concluir(tarefa.getId(), AUTH), HttpStatus.CONFLICT);
        assertThat(tarefa.getStatus()).isEqualTo(Status.EM_ANDAMENTO);
    }

    @Test
    void concluir_comFotoDoDepoisEntregaELimpaMotivoAnterior() {
        Tarefa tarefa = tarefa(Status.EM_ANDAMENTO);
        tarefa.setExigeFoto(true);
        tarefa.setMotivoReprovacao("Faltou a panela");
        when(jwtService.autenticado(AUTH)).thenReturn(filhoId);
        when(fotoRepository.existsByTarefaIdAndTipo(tarefa.getId(), FotoTarefa.Tipo.DEPOIS)).thenReturn(true);

        TarefaResponseDTO entregue = service.concluir(tarefa.getId(), AUTH);

        assertThat(entregue.status()).isEqualTo(Status.AGUARDANDO_APROVACAO);
        assertThat(entregue.motivoReprovacao()).isNull();
    }

    @Test
    void aprovar_paiConcluiTarefaEGuardaEventoDePontos() {
        Tarefa tarefa = tarefa(Status.AGUARDANDO_APROVACAO);
        logadoComo(UUID.randomUUID(), Parentesco.PAI);

        assertThat(service.aprovar(tarefa.getId(), AUTH).status()).isEqualTo(Status.CONCLUIDA);
        verify(outboxService).registrarTarefaAprovada(tarefa);
    }

    @Test
    void aprovar_segundaVezRecebe409ENaoGeraPontosEmDobro() {
        Tarefa tarefa = tarefa(Status.CONCLUIDA);
        logadoComo(maeId, Parentesco.MAE);

        assertStatus(() -> service.aprovar(tarefa.getId(), AUTH), HttpStatus.CONFLICT);
        verifyNoInteractions(outboxService);
    }

    @Test
    void aprovar_filhoNaoPodeAprovarAPropriaTarefa() {
        logadoComo(filhoId, Parentesco.FILHO);

        assertStatus(() -> service.aprovar(UUID.randomUUID(), AUTH), HttpStatus.FORBIDDEN);
        verify(tarefaRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void reprovar_devolveTarefaParaEmAndamentoComMotivo() {
        Tarefa tarefa = tarefa(Status.AGUARDANDO_APROVACAO);
        logadoComo(maeId, Parentesco.MAE);

        TarefaResponseDTO reprovada = service.reprovar(tarefa.getId(), " Faltou a panela ", AUTH);

        assertThat(reprovada.status()).isEqualTo(Status.EM_ANDAMENTO);
        assertThat(reprovada.motivoReprovacao()).isEqualTo("Faltou a panela");
        verifyNoInteractions(outboxService);
    }

    @Test
    void reprovar_tarefaQueNaoEstaAguardandoAprovacaoRecebe409() {
        Tarefa tarefa = tarefa(Status.PENDENTE);
        logadoComo(maeId, Parentesco.MAE);

        assertStatus(() -> service.reprovar(tarefa.getId(), null, AUTH), HttpStatus.CONFLICT);
    }

    @Test
    void buscarPorId_tarefaInexistenteRecebe404() {
        UUID id = UUID.randomUUID();
        when(jwtService.autenticado(AUTH)).thenReturn(filhoId);
        when(tarefaRepository.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> service.buscarPorId(id, AUTH), HttpStatus.NOT_FOUND);
    }

    @Test
    void deletar_maeRemoveTarefaEFotos() {
        Tarefa tarefa = tarefa(Status.PENDENTE);
        logadoComo(maeId, Parentesco.MAE);

        service.deletar(tarefa.getId(), AUTH);

        ArgumentCaptor<Tarefa> captor = ArgumentCaptor.forClass(Tarefa.class);
        verify(tarefaRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(tarefa.getId());
        verify(fotoRepository).deleteByTarefaId(tarefa.getId());
    }

    @Test
    void avisarPrazosProximos_avisaUmaVezSo() {
        Tarefa tarefa = tarefa(Status.PENDENTE);
        when(tarefaRepository.pendentesDeAvisoDePrazo(any())).thenReturn(List.of(tarefa));

        service.avisarPrazosProximos();

        assertThat(tarefa.getAvisoPrazoEnviado()).isTrue();
        verify(notificacaoService).notificar(eq(filhoId), eq("Prazo chegando"), any(), eq(tarefa.getId()));
    }
}
