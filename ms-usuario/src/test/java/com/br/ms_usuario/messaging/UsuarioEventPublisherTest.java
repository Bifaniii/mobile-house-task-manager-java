package com.br.ms_usuario.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsuarioEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UsuarioEventPublisher publisher;

    @Test
    void publicarUsuarioCriado_deveEnviarParaExchangeComRoutingKeyConfigurada() {
        ReflectionTestUtils.setField(publisher, "exchange", "usuario.exchange");
        ReflectionTestUtils.setField(publisher, "routingKeyUsuarioCriado", "usuario.criado");
        var evento = new UsuarioCriadoEvent(UUID.randomUUID(), "Ana", "ana@casa.com");

        publisher.publicarUsuarioCriado(evento);

        verify(rabbitTemplate).convertAndSend("usuario.exchange", "usuario.criado", evento);
    }
}
