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

    /** Tarefas criadas antes das famílias. O ms-usuario usa o mesmo id para as contas antigas. */
    public static final UUID FAMILIA_LEGADA = UUID.fromString("00000000-0000-0000-0000-000000000001");

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

    // As colunas novas aceitam nulo: o ddl-auto do SQLite não adiciona NOT NULL em tabela com dados.
    @Column(name = "familia_id")
    private UUID familiaId;

    private Instant prazo;

    private Integer pontos = 1;

    @Column(name = "exige_foto")
    private Boolean exigeFoto = false;

    @Column(name = "motivo_reprovacao", length = 500)
    private String motivoReprovacao;

    @Column(name = "rotina_id")
    private UUID rotinaId;

    @Column(name = "aviso_prazo_enviado")
    private Boolean avisoPrazoEnviado = false;

    //Método para o JPA preencher sozinho
    @PrePersist void aoCriar(){
        this.criadoEm = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }

    public int pontosOuPadrao() {
        return pontos == null ? 1 : pontos;
    }

    Tarefa(String titulo, String descricao, UUID usuarioId, UUID criadoPor) {
        status = Status.PENDENTE;
    }
}
