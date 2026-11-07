package com.br.ms_tarefa.messaging;

import com.br.ms_tarefa.config.RabbitConfig;
import com.br.ms_tarefa.domain.EventoOutbox;
import com.br.ms_tarefa.repository.EventoOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Envia os eventos guardados e só os marca como publicados depois da confirmação do broker. Se a
 * marcação falhar, o evento sai de novo: o consumidor descarta repetidos pelo id.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final long ESPERA_CONFIRMACAO_MS = 5000;

    private final EventoOutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(EventoOutboxRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPendentes() {
        for (EventoOutbox evento : repository.findTop50ByPublicadoEmIsNullOrderByCriadoEmAsc()) {
            try {
                Message mensagem = MessageBuilder.withBody(evento.getPayload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setMessageId(evento.getId().toString())
                        .setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT)
                        .build();

                rabbitTemplate.invoke(operacoes -> {
                    operacoes.send(RabbitConfig.EXCHANGE_TAREFA, evento.getRoutingKey(), mensagem);
                    operacoes.waitForConfirmsOrDie(ESPERA_CONFIRMACAO_MS);
                    return null;
                });
            } catch (Exception e) {
                // A ordem importa pouco, mas o broker provavelmente está fora: tenta tudo no próximo ciclo.
                log.warn("Evento {} segue guardado; RabbitMQ indisponível: {}", evento.getId(), e.getMessage());
                return;
            }

            evento.setPublicadoEm(Instant.now());
            repository.save(evento);
        }
    }
}
