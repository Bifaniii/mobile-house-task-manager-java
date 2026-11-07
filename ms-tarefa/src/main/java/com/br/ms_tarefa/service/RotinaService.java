package com.br.ms_tarefa.service;

import com.br.ms_tarefa.client.UsuarioClient;
import com.br.ms_tarefa.domain.Parentesco;
import com.br.ms_tarefa.domain.Rotina;
import com.br.ms_tarefa.domain.Tarefa;
import com.br.ms_tarefa.domain.dto.RotinaRequestDTO;
import com.br.ms_tarefa.domain.dto.RotinaResponseDTO;
import com.br.ms_tarefa.domain.dto.UsuarioResponseDTO;
import com.br.ms_tarefa.repository.RotinaRepository;
import com.br.ms_tarefa.repository.TarefaRepository;
import com.br.ms_tarefa.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RotinaService {

    private final RotinaRepository repository;
    private final TarefaRepository tarefaRepository;
    private final NotificacaoService notificacaoService;
    private final UsuarioClient usuarioClient;
    private final JwtService jwtService;

    public RotinaService(RotinaRepository repository,
                         TarefaRepository tarefaRepository,
                         NotificacaoService notificacaoService,
                         UsuarioClient usuarioClient,
                         JwtService jwtService) {
        this.repository = repository;
        this.tarefaRepository = tarefaRepository;
        this.notificacaoService = notificacaoService;
        this.usuarioClient = usuarioClient;
        this.jwtService = jwtService;
    }

    @Transactional
    public RotinaResponseDTO criar(RotinaRequestDTO dto, String auth) {
        UUID autor = exigirPaiOuMae(auth);
        // O ms-usuario só encontra pessoas da família do token: alguém de fora responde 404.
        List<UUID> participantes = dto.participantes().stream().distinct().toList();
        participantes.forEach(id -> usuarioClient.buscarUsuario(id, auth));

        Rotina rotina = new Rotina();
        rotina.setFamiliaId(jwtService.familiaDe(auth));
        rotina.setTitulo(dto.titulo().trim());
        rotina.setDescricao(dto.descricao());
        rotina.setPontos(dto.pontos() == null ? 1 : dto.pontos());
        rotina.setExigeFoto(Boolean.TRUE.equals(dto.exigeFoto()));
        rotina.setFrequencia(dto.frequencia());
        rotina.setParticipantes(new ArrayList<>(participantes));
        rotina.setPrazoHoras(dto.prazoHoras() == null ? 24 : dto.prazoHoras());
        rotina.setProximaExecucao(dto.inicio() == null ? Instant.now() : dto.inicio());
        rotina.setCriadoPor(autor);

        return toDTO(repository.save(rotina));
    }

    public List<RotinaResponseDTO> listar(String auth) {
        return repository.findByFamiliaId(jwtService.familiaDe(auth)).stream().map(this::toDTO).toList();
    }

    @Transactional
    public RotinaResponseDTO alternarAtiva(UUID id, String auth) {
        exigirPaiOuMae(auth);
        Rotina rotina = buscar(id, auth);
        rotina.setAtiva(!rotina.isAtiva());
        // Ao retomar, não cobra o período em que ficou pausada.
        if (rotina.isAtiva() && !rotina.getProximaExecucao().isAfter(Instant.now())) {
            rotina.setProximaExecucao(Instant.now());
        }
        return toDTO(repository.save(rotina));
    }

    @Transactional
    public void deletar(UUID id, String auth) {
        exigirPaiOuMae(auth);
        repository.delete(buscar(id, auth));
    }

    /** Chamado ao listar tarefas: no plano gratuito o serviço dorme, e o agendador sozinho perderia horários. */
    @Transactional
    public void gerarDevidas(UUID familiaId) {
        Instant agora = Instant.now();
        repository.findByFamiliaIdAndAtivaTrueAndProximaExecucaoLessThanEqual(familiaId, agora)
                .forEach(rotina -> gerar(rotina, agora));
    }

    @Transactional
    public void gerarTodasDevidas() {
        Instant agora = Instant.now();
        repository.findByAtivaTrueAndProximaExecucaoLessThanEqual(agora).forEach(rotina -> gerar(rotina, agora));
    }

    @Transactional
    public void removerParticipante(UUID usuarioId) {
        for (Rotina rotina : repository.findAll()) {
            if (rotina.getParticipantes().remove(usuarioId)) {
                // Nova lista: o Hibernate só percebe a mudança em atributo convertido se a referência mudar.
                rotina.setParticipantes(new ArrayList<>(rotina.getParticipantes()));
                if (rotina.getParticipantes().isEmpty()) rotina.setAtiva(false);
                repository.save(rotina);
            }
        }
    }

    private void gerar(Rotina rotina, Instant agora) {
        if (rotina.getParticipantes().isEmpty()) {
            rotina.setAtiva(false);
            repository.save(rotina);
            return;
        }

        UUID responsavel = proximoResponsavel(rotina, agora);

        Tarefa tarefa = new Tarefa();
        tarefa.setFamiliaId(rotina.getFamiliaId());
        tarefa.setTitulo(rotina.getTitulo());
        tarefa.setDescricao(rotina.getDescricao());
        tarefa.setUsuarioId(responsavel);
        tarefa.setCriadoPor(rotina.getCriadoPor());
        tarefa.setPontos(rotina.getPontos());
        tarefa.setExigeFoto(rotina.isExigeFoto());
        tarefa.setRotinaId(rotina.getId());
        tarefa.setPrazo(agora.plus(Duration.ofHours(rotina.getPrazoHoras())));
        tarefa = tarefaRepository.save(tarefa);

        rotina.setUltimoResponsavel(responsavel);
        rotina.setProximaExecucao(rotina.getFrequencia().proximaDepoisDe(rotina.getProximaExecucao(), agora));
        repository.save(rotina);

        notificacaoService.notificar(responsavel, "Sua vez", "\"" + rotina.getTitulo() + "\" ficou com você hoje.", tarefa.getId());
    }

    private UUID proximoResponsavel(Rotina rotina, Instant agora) {
        return RodizioJusto.escolher(rotina.getParticipantes(), tarefaRepository.findByFamiliaId(rotina.getFamiliaId()),
                rotina.getUltimoResponsavel(), agora);
    }

    private Rotina buscar(UUID id, String auth) {
        UUID familia = jwtService.familiaDe(auth);
        return repository.findById(id)
                .filter(r -> familia.equals(r.getFamiliaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rotina não encontrada"));
    }

    private UUID exigirPaiOuMae(String auth) {
        UUID id = jwtService.autenticado(auth);
        UsuarioResponseDTO usuario = usuarioClient.buscarUsuario(id, auth);
        if (usuario.parentesco() != Parentesco.MAE && usuario.parentesco() != Parentesco.PAI) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas pai ou mãe pode fazer isso");
        }
        return id;
    }

    private RotinaResponseDTO toDTO(Rotina rotina) {
        UUID proximo = !rotina.isAtiva() || rotina.getParticipantes().isEmpty() ? null : proximoResponsavel(rotina, Instant.now());
        return new RotinaResponseDTO(rotina.getId(), rotina.getTitulo(), rotina.getDescricao(), rotina.getPontos(),
                rotina.isExigeFoto(), rotina.getFrequencia(), rotina.getParticipantes(), rotina.getPrazoHoras(),
                rotina.getProximaExecucao(), rotina.isAtiva(), proximo);
    }
}
