package com.br.ms_carteira.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Saldo consolidado. O @Version impede que dois resgates simultâneos gastem o mesmo ponto. */
@Entity
@Table(name = "carteiras")
@Getter
@Setter
@NoArgsConstructor
public class Carteira {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;

    @Column(nullable = false)
    private int saldo = 0;

    @Version
    private Long versao;

    public Carteira(UUID usuarioId, UUID familiaId) {
        this.usuarioId = usuarioId;
        this.familiaId = familiaId;
    }
}
