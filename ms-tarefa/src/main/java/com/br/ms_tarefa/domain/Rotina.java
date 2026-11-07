package com.br.ms_tarefa.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Modelo que gera tarefas sozinho. Com mais de um participante, o rodízio justo escolhe quem faz. */
@Entity
@Table(name = "rotinas")
@Getter
@Setter
@NoArgsConstructor
public class Rotina {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false)
    private int pontos = 1;

    @Column(name = "exige_foto", nullable = false)
    private boolean exigeFoto = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequencia frequencia;

    @Convert(converter = ListaDeIds.class)
    @Column(nullable = false, length = 2000)
    private List<UUID> participantes = new ArrayList<>();

    @Column(name = "prazo_horas", nullable = false)
    private int prazoHoras = 24;

    @Column(name = "proxima_execucao", nullable = false)
    private Instant proximaExecucao;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_por", nullable = false)
    private UUID criadoPor;

    @Column(name = "ultimo_responsavel")
    private UUID ultimoResponsavel;

    @Converter
    public static class ListaDeIds implements AttributeConverter<List<UUID>, String> {
        @Override
        public String convertToDatabaseColumn(List<UUID> ids) {
            return ids.stream().map(UUID::toString).collect(Collectors.joining(","));
        }

        @Override
        public List<UUID> convertToEntityAttribute(String texto) {
            if (texto == null || texto.isBlank()) return new ArrayList<>();
            return new ArrayList<>(Arrays.stream(texto.split(",")).map(UUID::fromString).toList());
        }
    }
}
