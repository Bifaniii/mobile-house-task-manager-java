package com.br.ms_tarefa.service;

import com.br.ms_tarefa.client.UsuarioClient;
import com.br.ms_tarefa.domain.FotoTarefa;
import com.br.ms_tarefa.domain.Parentesco;
import com.br.ms_tarefa.domain.Status;
import com.br.ms_tarefa.domain.Tarefa;
import com.br.ms_tarefa.domain.dto.TarefaRequestDTO;
import com.br.ms_tarefa.domain.dto.TarefaResponseDTO;
import com.br.ms_tarefa.domain.dto.UsuarioResponseDTO;
import com.br.ms_tarefa.repository.FotoTarefaRepository;
import com.br.ms_tarefa.repository.NotificacaoRepository;
import com.br.ms_tarefa.repository.TarefaRepository;
import com.br.ms_tarefa.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TarefaService {

    private static final Duration ANTECEDENCIA_AVISO = Duration.ofHours(3);

    private final TarefaRepository tarefaRepository;
    private final UsuarioClient usuarioClient;
    private final JwtService jwtService;
    private final NotificacaoService notificacaoService;
    private final NotificacaoRepository notificacaoRepository;
    private final OutboxService outboxService;
    private final RotinaService rotinaService;
    private final FotoTarefaRepository fotoRepository;

    public TarefaService(TarefaRepository tarefaRepository,
                         UsuarioClient usuarioClient,
                         JwtService jwtService,
                         NotificacaoService notificacaoService,
                         NotificacaoRepository notificacaoRepository,
                         OutboxService outboxService,
                         RotinaService rotinaService,
                         FotoTarefaRepository fotoRepository) {
        this.tarefaRepository = tarefaRepository;
        this.usuarioClient = usuarioClient;
        this.jwtService = jwtService;
        this.notificacaoService = notificacaoService;
        this.notificacaoRepository = notificacaoRepository;
        this.outboxService = outboxService;
        this.rotinaService = rotinaService;
        this.fotoRepository = fotoRepository;
    }

    private UUID exigirPaiOuMae(String auth){
        UUID id = jwtService.autenticado(auth);
        UsuarioResponseDTO usuario = usuarioClient.buscarUsuario(id,auth);
        if(usuario.parentesco() != Parentesco.MAE && usuario.parentesco() != Parentesco.PAI){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas pai ou mãe pode fazer isso");
        }
        return id;
    }

    private void exigirResponsavel(Tarefa tarefa, String auth){
        if(!tarefa.getUsuarioId().equals(jwtService.autenticado(auth))){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é responsável pela tarefa");
        }

    }

    @Transactional
    public TarefaResponseDTO criar(TarefaRequestDTO dto, String auth){
        UUID autor = exigirPaiOuMae(auth);
        // O ms-usuario só encontra pessoas da família do token: alguém de fora responde 404.
        usuarioClient.buscarUsuario(dto.usuarioId(), auth);

        if (dto.prazo() != null && dto.prazo().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O prazo precisa ser uma data futura");
        }

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(dto.titulo());
        tarefa.setDescricao(dto.descricao());
        tarefa.setUsuarioId(dto.usuarioId());
        tarefa.setCriadoPor(autor);
        tarefa.setFamiliaId(jwtService.familiaDe(auth));
        tarefa.setPrazo(dto.prazo());
        tarefa.setPontos(dto.pontos() == null ? 1 : dto.pontos());
        tarefa.setExigeFoto(Boolean.TRUE.equals(dto.exigeFoto()));

        Tarefa salva = tarefaRepository.save(tarefa);
        avisar(dto.usuarioId(), autor, "Tarefa nova", "\"" + dto.titulo() + "\" ficou com você.", salva);
        return toDTO(salva);
    }

    public List<TarefaResponseDTO> listarTarefa(String auth) {
        jwtService.autenticado(auth);
        UUID familia = jwtService.familiaDe(auth);
        rotinaService.gerarDevidas(familia);
        return tarefaRepository.findByFamiliaId(familia).stream().map(this::toDTO).toList();
    }

    public List<TarefaResponseDTO> listarPorUsuario(UUID usuarioId, String auth) {
        jwtService.autenticado(auth);
        return tarefaRepository.findByFamiliaIdAndUsuarioId(jwtService.familiaDe(auth), usuarioId).stream().map(this::toDTO).toList();
    }

    public TarefaResponseDTO buscarPorId(UUID id, String auth) {
        jwtService.autenticado(auth);
        return toDTO(buscarTarefa(id, auth));
    }


    @Transactional
    public TarefaResponseDTO iniciar(UUID id, String auth) {
        Tarefa tarefa = buscarTarefa(id, auth);
        exigirResponsavel(tarefa, auth);

        if (tarefa.getStatus() != Status.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só é possível iniciar uma tarefa pendente");
        }

        tarefa.setStatus(Status.EM_ANDAMENTO);
        return toDTO(tarefaRepository.save(tarefa));
    }

    @Transactional
    public TarefaResponseDTO concluir(UUID id, String auth) {
        Tarefa tarefa = buscarTarefa(id, auth);
        exigirResponsavel(tarefa, auth);

        if (tarefa.getStatus() != Status.PENDENTE && tarefa.getStatus() != Status.EM_ANDAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta tarefa não está em andamento");
        }
        if (Boolean.TRUE.equals(tarefa.getExigeFoto())
                && !fotoRepository.existsByTarefaIdAndTipo(id, FotoTarefa.Tipo.DEPOIS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta tarefa precisa de uma foto do depois antes da entrega");
        }

        tarefa.setStatus(Status.AGUARDANDO_APROVACAO);
        tarefa.setMotivoReprovacao(null);
        Tarefa salva = tarefaRepository.save(tarefa);
        avisar(tarefa.getCriadoPor(), tarefa.getUsuarioId(), "Tarefa entregue", "\"" + tarefa.getTitulo() + "\" está esperando sua aprovação.", salva);
        return toDTO(salva);
    }

    @Transactional
    public TarefaResponseDTO aprovar(UUID id, String auth) {
        UUID quemAprovou = exigirPaiOuMae(auth);
        Tarefa tarefa = buscarTarefa(id, auth);

        exigirAguardandoAprovacao(tarefa);

        tarefa.setStatus(Status.CONCLUIDA);
        Tarefa salva = tarefaRepository.save(tarefa);
        outboxService.registrarTarefaAprovada(salva);
        int pontos = tarefa.pontosOuPadrao();
        avisar(tarefa.getUsuarioId(), quemAprovou, "Tarefa aprovada",
                "\"" + tarefa.getTitulo() + "\" foi aprovada. Você ganhou " + pontos + (pontos == 1 ? " ponto." : " pontos."), salva);
        return toDTO(salva);
    }

    @Transactional
    public TarefaResponseDTO reprovar(UUID id, String motivo, String auth) {
        UUID quemReprovou = exigirPaiOuMae(auth);
        Tarefa tarefa = buscarTarefa(id, auth);

        exigirAguardandoAprovacao(tarefa);

        String motivoLimpo = motivo == null || motivo.isBlank() ? null : motivo.trim();
        tarefa.setStatus(Status.EM_ANDAMENTO);
        tarefa.setMotivoReprovacao(motivoLimpo);
        Tarefa salva = tarefaRepository.save(tarefa);
        avisar(tarefa.getUsuarioId(), quemReprovou, "Precisa refazer",
                "\"" + tarefa.getTitulo() + "\" voltou para você." + (motivoLimpo == null ? "" : " Motivo: " + motivoLimpo), salva);
        return toDTO(salva);
    }

    @Transactional
    public void deletar(UUID id, String auth) {
        exigirPaiOuMae(auth);
        Tarefa tarefa = buscarTarefa(id, auth);

        fotoRepository.deleteByTarefaId(id);
        notificacaoRepository.deleteByTarefaId(id);
        tarefaRepository.delete(tarefa);
    }

    /** Roda no agendador. O aviso sai uma vez só por tarefa. */
    @Transactional
    public void avisarPrazosProximos() {
        for (Tarefa tarefa : tarefaRepository.pendentesDeAvisoDePrazo(Instant.now().plus(ANTECEDENCIA_AVISO))) {
            tarefa.setAvisoPrazoEnviado(true);
            tarefaRepository.save(tarefa);
            notificacaoService.notificar(tarefa.getUsuarioId(), "Prazo chegando",
                    "\"" + tarefa.getTitulo() + "\" precisa ser entregue em breve.", tarefa.getId());
        }
    }

    private void avisar(UUID destinatario, UUID autor, String titulo, String mensagem, Tarefa tarefa) {
        if (!destinatario.equals(autor)) {
            notificacaoService.notificar(destinatario, titulo, mensagem, tarefa.getId());
        }
    }

    /** Tarefa de outra família responde 404, sem revelar que existe. */
    private Tarefa buscarTarefa(UUID id, String auth) {
        UUID familia = jwtService.familiaDe(auth);
        return tarefaRepository.findById(id)
                .filter(t -> Objects.equals(familia, t.getFamiliaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tarefa não encontrada com id: " + id));
    }

    private void exigirAguardandoAprovacao(Tarefa tarefa) {
        if (tarefa.getStatus() != Status.AGUARDANDO_APROVACAO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta tarefa não está aguardando aprovação");
        }
    }

    private TarefaResponseDTO toDTO(Tarefa tarefa) {
        return new TarefaResponseDTO(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getUsuarioId(),
                tarefa.getCriadoPor(),
                tarefa.getStatus(),
                tarefa.getPrazo(),
                tarefa.pontosOuPadrao(),
                Boolean.TRUE.equals(tarefa.getExigeFoto()),
                tarefa.getMotivoReprovacao(),
                tarefa.getRotinaId()
        );
    }
}
