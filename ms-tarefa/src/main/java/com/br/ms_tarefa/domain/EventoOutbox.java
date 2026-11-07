package com.br.ms_tarefa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento gravado na mesma transação da mudança que o originou e enviado ao broker depois.
 * Assim, uma tarefa aprovada nunca deixa de gerar pontos porque o RabbitMQ estava fora do ar.
 */
@Entity
@Table(name = "eventos_outbox")
@Getter
@Setter
@NoArgsConstructor
public class EventoOutbox {

    @Id
    private UUID id;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Column(nullable = false, length = 2000)
    private String payload;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "publicado_em")
    private Instant publicadoEm;
}
