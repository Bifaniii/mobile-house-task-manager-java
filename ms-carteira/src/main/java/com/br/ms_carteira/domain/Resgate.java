package com.br.ms_carteira.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resgates")
@Getter
@Setter
@NoArgsConstructor
public class Resgate {

    public enum Status { PENDENTE, APROVADO, RECUSADO }

    @Id
    private UUID id;

    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "premio_id", nullable = false)
    private UUID premioId;

    // Cópias: o resgate guarda o que valia na hora, mesmo que o prêmio mude depois.
    @Column(name = "premio_nome", nullable = false)
    private String premioNome;

    @Column(nullable = false)
    private int custo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "decidido_por")
    private UUID decididoPor;
}
