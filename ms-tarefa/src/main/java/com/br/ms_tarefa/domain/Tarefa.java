package com.br.ms_tarefa.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "tarefas")
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String titulo;
    private String descricao;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "criado_por",nullable = false)
    private UUID criadoPor;

    @Column(name = "criado_em", nullable = false,  updatable = false)
    private ZonedDateTime criadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDENTE;

    //Método para o JPA preencher sozinho
    @PrePersist void aoCriar(){
        this.criadoEm = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }

    Tarefa(String titulo, String descricao, UUID usuarioId, UUID criadoPor) {
        status = Status.PENDENTE;
    }
}
