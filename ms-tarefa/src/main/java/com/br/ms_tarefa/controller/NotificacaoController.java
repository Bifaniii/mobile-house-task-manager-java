package com.br.ms_tarefa.controller;

import com.br.ms_tarefa.domain.dto.NotificacaoResponseDTO;
import com.br.ms_tarefa.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<NotificacaoResponseDTO>> minhas(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(service.minhas(auth));
    }

    @PatchMapping("/lidas")
    public ResponseEntity<Void> marcarLidas(@RequestHeader(value = "Authorization", required = false) String auth) {
        service.marcarTodasComoLidas(auth);
        return ResponseEntity.noContent().build();
    }
}
