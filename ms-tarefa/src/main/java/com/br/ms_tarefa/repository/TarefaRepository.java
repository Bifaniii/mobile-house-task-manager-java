package com.br.ms_tarefa.repository;

import com.br.ms_tarefa.domain.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

    List<Tarefa> findByFamiliaId(UUID familiaId);

    List<Tarefa> findByFamiliaIdAndUsuarioId(UUID familiaId, UUID usuarioId);

    @Query("""
            select t from Tarefa t
            where t.prazo is not null and t.prazo <= :limite
              and t.status in (com.br.ms_tarefa.domain.Status.PENDENTE, com.br.ms_tarefa.domain.Status.EM_ANDAMENTO)
              and (t.avisoPrazoEnviado is null or t.avisoPrazoEnviado = false)
            """)
    List<Tarefa> pendentesDeAvisoDePrazo(@Param("limite") Instant limite);

    @Modifying
    @Query("update Tarefa t set t.familiaId = :familia where t.familiaId is null")
    int migrarSemFamilia(@Param("familia") UUID familia);
}
