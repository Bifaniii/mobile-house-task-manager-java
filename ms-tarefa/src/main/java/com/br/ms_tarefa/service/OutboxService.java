package com.br.ms_tarefa.service;

import com.br.ms_tarefa.domain.EventoOutbox;
import com.br.ms_tarefa.domain.Tarefa;
import com.br.ms_tarefa.repository.EventoOutboxRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxService {

    public static final String ROTA_TAREFA_APROVADA = "tarefa.aprovada";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final EventoOutboxRepository repository;

    public OutboxService(EventoOutboxRepository repository) {
        this.repository = repository;
    }

    /** Deve ser chamado dentro da transação da aprovação: ou os dois gravam, ou nenhum. */
    public void registrarTarefaAprovada(Tarefa tarefa) {
        UUID eventoId = UUID.randomUUID();

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("eventoId", eventoId.toString());
        dados.put("tarefaId", tarefa.getId().toString());
        dados.put("usuarioId", tarefa.getUsuarioId().toString());
        dados.put("familiaId", tarefa.getFamiliaId().toString());
        dados.put("pontos", tarefa.pontosOuPadrao());
        dados.put("titulo", tarefa.getTitulo());

        EventoOutbox evento = new EventoOutbox();
        evento.setId(eventoId);
        evento.setRoutingKey(ROTA_TAREFA_APROVADA);
        evento.setPayload(JSON.writeValueAsString(dados));
        repository.save(evento);
    }
}
