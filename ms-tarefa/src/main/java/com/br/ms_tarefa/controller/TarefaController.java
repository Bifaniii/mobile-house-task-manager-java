package com.br.ms_tarefa.controller;

import com.br.ms_tarefa.domain.dto.TarefaRequestDTO;
import com.br.ms_tarefa.domain.dto.TarefaResponseDTO;
import com.br.ms_tarefa.service.TarefaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    
    // CRIAÇÃO  (só mãe ou pai)
    

    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(
            @Valid @RequestBody TarefaRequestDTO dto,
            @RequestHeader(value = "Authorization", required = true) String auth) {

        TarefaResponseDTO criada = service.criar(dto, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    
    // CONSULTAS
    

    @GetMapping
    public ResponseEntity<List<TarefaResponseDTO>> listar(
            @RequestHeader(value = "Authorization", required = false) String auth) {

        return ResponseEntity.ok(service.listarTarefa(auth));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> buscarPorId(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        return ResponseEntity.ok(service.buscarPorId(id, auth));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<TarefaResponseDTO>> listarPorUsuario(
            @PathVariable UUID usuarioId,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        return ResponseEntity.ok(service.listarPorUsuario(usuarioId, auth));
    }

    
    // AÇÕES DO RESPONSÁVEL
    

    @PatchMapping("/{id}/iniciar")
    public ResponseEntity<TarefaResponseDTO> iniciar(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        return ResponseEntity.ok(service.iniciar(id, auth));
    }

    /** O responsável marca como feita -> vai para AGUARDANDO_APROVACAO, não para CONCLUIDA. */
    @PatchMapping("/{id}/concluir")
    public ResponseEntity<TarefaResponseDTO> concluir(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        return ResponseEntity.ok(service.concluir(id, auth));
    }

    
    // AÇÕES DA MÃE / DO PAI
    

    @PatchMapping("/{id}/aprovar")
    public ResponseEntity<TarefaResponseDTO> aprovar(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        return ResponseEntity.ok(service.aprovar(id, auth));
    }

    @PatchMapping("/{id}/reprovar")
    public ResponseEntity<TarefaResponseDTO> reprovar(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        return ResponseEntity.ok(service.reprovar(id, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        service.deletar(id, auth);
        return ResponseEntity.noContent().build();
    }
}
