package com.br.ms_tarefa.service;

import com.br.ms_tarefa.client.UsuarioClient;
import com.br.ms_tarefa.domain.Parentesco;
import com.br.ms_tarefa.domain.Status;
import com.br.ms_tarefa.domain.Tarefa;
import com.br.ms_tarefa.domain.dto.TarefaRequestDTO;
import com.br.ms_tarefa.domain.dto.TarefaResponseDTO;
import com.br.ms_tarefa.domain.dto.UsuarioResponseDTO;
import com.br.ms_tarefa.repository.TarefaRepository;
import com.br.ms_tarefa.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class TarefaService {
    private final TarefaRepository tarefaRepository;
    private final UsuarioClient usuarioClient;
    private final JwtService jwtService;

    public TarefaService(TarefaRepository tarefaRepository, UsuarioClient usuarioClient, JwtService jwtService) {
        this.tarefaRepository = tarefaRepository;
        this.usuarioClient = usuarioClient;
        this.jwtService = jwtService;
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
        usuarioClient.buscarUsuario(dto.usuarioId(), auth);

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(dto.titulo());
        tarefa.setDescricao(dto.descricao());
        tarefa.setUsuarioId(dto.usuarioId());
        tarefa.setCriadoPor(autor);

        return toDTO(tarefaRepository.save(tarefa));
    }

    public List<TarefaResponseDTO> listarTarefa(String auth) {
        jwtService.autenticado(auth);
        return tarefaRepository.findAll().stream().map(this::toDTO).toList();
    }

    public List<TarefaResponseDTO> listarPorUsuario(UUID usuarioId, String auth) {
        jwtService.autenticado(auth);
        return tarefaRepository.findByUsuarioId(usuarioId).stream().map(this::toDTO).toList();
    }

    public TarefaResponseDTO buscarPorId(UUID id, String auth) {
        jwtService.autenticado(auth);
        return toDTO(buscarTarefa(id));
    }


    @Transactional
    public TarefaResponseDTO iniciar(UUID id, String auth) {
        Tarefa tarefa = buscarTarefa(id);
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
        Tarefa tarefa = buscarTarefa(id);
        exigirResponsavel(tarefa, auth);

        if (tarefa.getStatus() != Status.PENDENTE && tarefa.getStatus() != Status.EM_ANDAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta tarefa não está em andamento");
        }

        tarefa.setStatus(Status.AGUARDANDO_APROVACAO);
        return toDTO(tarefaRepository.save(tarefa));
    }

    @Transactional
    public TarefaResponseDTO aprovar(UUID id, String auth) {
        exigirPaiOuMae(auth);
        Tarefa tarefa = buscarTarefa(id);

        exigirAguardandoAprovacao(tarefa);

        tarefa.setStatus(Status.CONCLUIDA);
        return toDTO(tarefaRepository.save(tarefa));
    }

    @Transactional
    public TarefaResponseDTO reprovar(UUID id, String auth) {
        exigirPaiOuMae(auth);
        Tarefa tarefa = buscarTarefa(id);

        exigirAguardandoAprovacao(tarefa);

        tarefa.setStatus(Status.EM_ANDAMENTO);
        return toDTO(tarefaRepository.save(tarefa));
    }

    @Transactional
    public void deletar(UUID id, String auth) {
        exigirPaiOuMae(auth);
        Tarefa tarefa = buscarTarefa(id);

        tarefaRepository.delete(tarefa);
    }

    private Tarefa buscarTarefa(UUID id) {
        return tarefaRepository.findById(id)
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
                tarefa.getStatus()
        );
    }



}
