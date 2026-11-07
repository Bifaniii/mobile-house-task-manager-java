package com.br.ms_tarefa.messaging;

import com.br.ms_tarefa.config.RabbitConfig;
import com.br.ms_tarefa.service.RotinaService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/** Tira da rotina quem saiu da família, para o rodízio não passar tarefa a uma conta que não existe mais. */
@Component
public class UsuarioRemovidoListener {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RotinaService rotinaService;

    public UsuarioRemovidoListener(RotinaService rotinaService) {
        this.rotinaService = rotinaService;
    }

    @RabbitListener(queues = RabbitConfig.FILA_USUARIO_REMOVIDO)
    public void aoRemover(Message mensagem) {
        rotinaService.removerParticipante(lerUsuario(mensagem));
    }

    private UUID lerUsuario(Message mensagem) {
        try {
            return UUID.fromString(JSON.readTree(mensagem.getBody()).get("usuarioId").asString());
        } catch (Exception e) {
            // Tentar de novo não conserta uma mensagem malformada.
            throw new AmqpRejectAndDontRequeueException("Evento usuario.removido inválido", e);
        }
    }
}
