package com.br.ms_usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "familias", uniqueConstraints = @UniqueConstraint(name = "uk_codigo_convite", columnNames = "codigo_convite"))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Familia {

    /** Família das contas criadas antes de o conceito existir. O ms-tarefa usa o mesmo id nas tarefas antigas. */
    public static final UUID ID_LEGADO = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "codigo_convite", nullable = false)
    private String codigoConvite;
}
