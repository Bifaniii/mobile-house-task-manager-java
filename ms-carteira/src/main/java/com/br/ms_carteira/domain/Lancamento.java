package com.br.ms_carteira.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Linha do extrato. É imutável: correção vira um novo lançamento, nunca uma edição.
 * A coluna origem é única, então o mesmo fato não gera pontos duas vezes.
 */
@Entity
@Immutable
@Table(name = "lancamentos", uniqueConstraints = @UniqueConstraint(name = "uk_lancamento_origem", columnNames = "origem"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lancamento {

    public enum Tipo { CREDITO_TAREFA, RESGATE, ESTORNO }

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(nullable = false)
    private int valor;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private String origem;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public Lancamento(UUID usuarioId, UUID familiaId, Tipo tipo, int valor, String descricao, String origem) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.familiaId = familiaId;
        this.tipo = tipo;
        this.valor = valor;
        this.descricao = descricao;
        this.origem = origem;
        this.criadoEm = Instant.now();
    }
}
