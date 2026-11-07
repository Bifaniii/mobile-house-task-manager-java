package com.br.ms_carteira.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "premios")
@Getter
@Setter
@NoArgsConstructor
public class Premio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int custo;

    // Removido fica inativo: resgates antigos continuam apontando para ele.
    @Column(nullable = false)
    private boolean ativo = true;
}
