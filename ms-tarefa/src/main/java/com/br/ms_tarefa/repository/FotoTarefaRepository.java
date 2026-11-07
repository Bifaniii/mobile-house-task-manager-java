package com.br.ms_tarefa.repository;

import com.br.ms_tarefa.domain.FotoTarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FotoTarefaRepository extends JpaRepository<FotoTarefa, UUID> {

    Optional<FotoTarefa> findByTarefaIdAndTipo(UUID tarefaId, FotoTarefa.Tipo tipo);

    boolean existsByTarefaIdAndTipo(UUID tarefaId, FotoTarefa.Tipo tipo);

    /** Só os tipos: evita carregar os bytes das fotos para saber quais existem. */
    @Query("select f.tipo from FotoTarefa f where f.tarefaId = :tarefa")
    List<FotoTarefa.Tipo> tiposDaTarefa(@Param("tarefa") UUID tarefa);

    void deleteByTarefaId(UUID tarefaId);
}
