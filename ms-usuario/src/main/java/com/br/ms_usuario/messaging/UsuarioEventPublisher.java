package com.br.ms_usuario.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UsuarioEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routingkey.usuario-criado}")
    private String routingKeyUsuarioCriado;

    public UsuarioEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarUsuarioCriado(UsuarioCriadoEvent evento) {
        rabbitTemplate.convertAndSend(exchange, routingKeyUsuarioCriado, evento);
    }
}