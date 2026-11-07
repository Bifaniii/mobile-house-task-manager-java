package com.br.ms_tarefa.repository;

import com.br.ms_tarefa.domain.EventoOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoOutboxRepository extends JpaRepository<EventoOutbox, UUID> {

    List<EventoOutbox> findTop50ByPublicadoEmIsNullOrderByCriadoEmAsc();
}
