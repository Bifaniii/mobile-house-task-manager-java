package com.br.ms_tarefa.config;

import com.br.ms_tarefa.domain.Tarefa;
import com.br.ms_tarefa.messaging.OutboxPublisher;
import com.br.ms_tarefa.repository.TarefaRepository;
import com.br.ms_tarefa.service.RotinaService;
import com.br.ms_tarefa.service.TarefaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableScheduling
public class Agendador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Agendador.class);

    private final RotinaService rotinaService;
    private final TarefaService tarefaService;
    private final OutboxPublisher outboxPublisher;
    private final TarefaRepository tarefaRepository;

    public Agendador(RotinaService rotinaService,
                     TarefaService tarefaService,
                     OutboxPublisher outboxPublisher,
                     TarefaRepository tarefaRepository) {
        this.rotinaService = rotinaService;
        this.tarefaService = tarefaService;
        this.outboxPublisher = outboxPublisher;
        this.tarefaRepository = tarefaRepository;
    }

    /** Coloca as tarefas antigas na família legada, a mesma que o ms-usuario dá às contas antigas. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migradas = tarefaRepository.migrarSemFamilia(Tarefa.FAMILIA_LEGADA);
        if (migradas > 0) log.info("{} tarefas antigas movidas para a família legada", migradas);
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 60_000)
    public void rotinasEPrazos() {
        rotinaService.gerarTodasDevidas();
        tarefaService.avisarPrazosProximos();
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 15_000)
    public void eventos() {
        outboxPublisher.publicarPendentes();
    }
}
