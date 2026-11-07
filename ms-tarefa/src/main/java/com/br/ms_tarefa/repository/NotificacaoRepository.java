package com.br.ms_tarefa.repository;

import com.br.ms_tarefa.domain.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    List<Notificacao> findTop50ByUsuarioIdOrderByCriadaEmDesc(UUID usuarioId);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.usuarioId = :usuario and n.lida = false")
    int marcarTodasComoLidas(@Param("usuario") UUID usuario);

    void deleteByTarefaId(UUID tarefaId);
}
