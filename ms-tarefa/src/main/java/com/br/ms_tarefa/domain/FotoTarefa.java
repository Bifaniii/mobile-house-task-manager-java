package com.br.ms_tarefa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "fotos_tarefa", uniqueConstraints = @UniqueConstraint(name = "uk_foto_tarefa_tipo", columnNames = {"tarefa_id", "tipo"}))
@Getter
@Setter
@NoArgsConstructor
public class FotoTarefa {

    public enum Tipo { ANTES, DEPOIS }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tarefa_id", nullable = false)
    private UUID tarefaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    // VARBINARY em vez de @Lob: o driver do SQLite não implementa getBlob.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, length = 3_000_000)
    private byte[] conteudo;
}
