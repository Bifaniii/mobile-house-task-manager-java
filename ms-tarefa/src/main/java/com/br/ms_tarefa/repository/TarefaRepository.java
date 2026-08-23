package com.br.ms_tarefa.repository;

import com.br.ms_tarefa.domain.Status;
import com.br.ms_tarefa.domain.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

    List<Tarefa> findByUsuarioId(UUID usuarioId);

    List<Tarefa> findByStatus(Status status);
}