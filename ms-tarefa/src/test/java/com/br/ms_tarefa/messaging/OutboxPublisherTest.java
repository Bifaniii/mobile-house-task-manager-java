package com.br.ms_tarefa.messaging;

import com.br.ms_tarefa.domain.EventoOutbox;
import com.br.ms_tarefa.repository.EventoOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private EventoOutboxRepository repository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxPublisher publisher;

    private EventoOutbox evento() {
        EventoOutbox evento = new EventoOutbox();
        evento.setId(UUID.randomUUID());
        evento.setRoutingKey("tarefa.aprovada");
        evento.setPayload("{}");
        return evento;
    }

    @Test
    void marcaComoPublicadoDepoisDoEnvio() {
        EventoOutbox evento = evento();
        when(repository.findTop50ByPublicadoEmIsNullOrderByCriadoEmAsc()).thenReturn(List.of(evento));

        publisher.publicarPendentes();

        verify(rabbitTemplate).invoke(any());
        assertThat(evento.getPublicadoEm()).isNotNull();
        verify(repository).save(evento);
    }

    @Test
    void comBrokerForaGuardaOEventoParaOProximoCiclo() {
        EventoOutbox evento = evento();
        when(repository.findTop50ByPublicadoEmIsNullOrderByCriadoEmAsc()).thenReturn(List.of(evento, evento()));
        when(rabbitTemplate.invoke(any())).thenThrow(new AmqpConnectException(new RuntimeException("connection refused")));

        publisher.publicarPendentes();

        assertThat(evento.getPublicadoEm()).isNull();
        verify(repository, never()).save(any());
        verify(rabbitTemplate, times(1)).invoke(any());
    }
}
