package com.br.ms_tarefa.repository;

import com.br.ms_tarefa.domain.Rotina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RotinaRepository extends JpaRepository<Rotina, UUID> {

    List<Rotina> findByFamiliaId(UUID familiaId);

    List<Rotina> findByAtivaTrueAndProximaExecucaoLessThanEqual(Instant agora);

    List<Rotina> findByFamiliaIdAndAtivaTrueAndProximaExecucaoLessThanEqual(UUID familiaId, Instant agora);
}
