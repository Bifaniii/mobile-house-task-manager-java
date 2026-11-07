package com.br.ms_tarefa.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_TAREFA = "tarefa.exchange";
    public static final String FILA_USUARIO_REMOVIDO = "tarefa.usuario-removido";

    @Bean
    public TopicExchange tarefaExchange() {
        return new TopicExchange(EXCHANGE_TAREFA);
    }

    @Bean
    public TopicExchange usuarioExchange() {
        return new TopicExchange("usuario.exchange");
    }

    @Bean
    public Queue filaUsuarioRemovido() {
        return new Queue(FILA_USUARIO_REMOVIDO, true);
    }

    @Bean
    public Binding bindingUsuarioRemovido() {
        return BindingBuilder.bind(filaUsuarioRemovido()).to(usuarioExchange()).with("usuario.removido");
    }
}
