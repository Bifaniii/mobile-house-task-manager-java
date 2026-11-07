package com.br.ms_carteira.messaging;

import com.br.ms_carteira.service.CarteiraService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * Credita os pontos das tarefas aprovadas. Falha temporária devolve a mensagem à fila; como o crédito
 * é idempotente, reprocessar é seguro. Mensagem malformada é descartada, porque nunca daria certo.
 */
@Configuration
public class TarefaAprovadaListener {

    static final String FILA = "carteira.tarefa-aprovada";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final CarteiraService carteira;

    public TarefaAprovadaListener(CarteiraService carteira) {
        this.carteira = carteira;
    }

    @Bean
    public TopicExchange tarefaExchange() {
        return new TopicExchange("tarefa.exchange");
    }

    @Bean
    public Queue filaTarefaAprovada() {
        return new Queue(FILA, true);
    }

    @Bean
    public Binding bindingTarefaAprovada() {
        return BindingBuilder.bind(filaTarefaAprovada()).to(tarefaExchange()).with("tarefa.aprovada");
    }

    @RabbitListener(queues = FILA)
    public void aoAprovar(Message mensagem) {
        UUID eventoId;
        UUID usuarioId;
        UUID familiaId;
        int pontos;
        String titulo;
        try {
            JsonNode dados = JSON.readTree(mensagem.getBody());
            eventoId = UUID.fromString(dados.get("eventoId").asString());
            usuarioId = UUID.fromString(dados.get("usuarioId").asString());
            familiaId = UUID.fromString(dados.get("familiaId").asString());
            pontos = dados.get("pontos").asInt();
            titulo = dados.get("titulo").asString();
        } catch (Exception e) {
            throw new AmqpRejectAndDontRequeueException("Evento tarefa.aprovada inválido", e);
        }
        if (pontos <= 0) {
            throw new AmqpRejectAndDontRequeueException("Evento tarefa.aprovada com pontos inválidos");
        }

        carteira.creditarTarefa(eventoId, usuarioId, familiaId, pontos, titulo);
    }
}
