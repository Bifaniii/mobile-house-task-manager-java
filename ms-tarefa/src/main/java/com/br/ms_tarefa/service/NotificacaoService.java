package com.br.ms_tarefa.service;

import com.br.ms_tarefa.domain.Notificacao;
import com.br.ms_tarefa.domain.dto.NotificacaoResponseDTO;
import com.br.ms_tarefa.repository.NotificacaoRepository;
import com.br.ms_tarefa.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificacaoService {

    private final NotificacaoRepository repository;
    private final JwtService jwtService;

    public NotificacaoService(NotificacaoRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public void notificar(UUID usuarioId, String titulo, String mensagem, UUID tarefaId) {
        Notificacao notificacao = new Notificacao();
        notificacao.setUsuarioId(usuarioId);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem(mensagem);
        notificacao.setTarefaId(tarefaId);
        repository.save(notificacao);
    }

    public List<NotificacaoResponseDTO> minhas(String auth) {
        return repository.findTop50ByUsuarioIdOrderByCriadaEmDesc(jwtService.autenticado(auth)).stream()
                .map(n -> new NotificacaoResponseDTO(n.getId(), n.getTitulo(), n.getMensagem(), n.getTarefaId(), n.isLida(), n.getCriadaEm()))
                .toList();
    }

    @Transactional
    public void marcarTodasComoLidas(String auth) {
        repository.marcarTodasComoLidas(jwtService.autenticado(auth));
    }
}
