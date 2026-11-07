package com.br.ms_tarefa.controller;

import com.br.ms_tarefa.domain.dto.RotinaRequestDTO;
import com.br.ms_tarefa.domain.dto.RotinaResponseDTO;
import com.br.ms_tarefa.service.RotinaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rotinas")
public class RotinaController {

    private final RotinaService service;

    public RotinaController(RotinaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RotinaResponseDTO> criar(@Valid @RequestBody RotinaRequestDTO dto,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto, auth));
    }

    @GetMapping
    public ResponseEntity<List<RotinaResponseDTO>> listar(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(service.listar(auth));
    }

    @PatchMapping("/{id}/ativa")
    public ResponseEntity<RotinaResponseDTO> alternarAtiva(@PathVariable UUID id,
                                                           @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(service.alternarAtiva(id, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id,
                                        @RequestHeader(value = "Authorization", required = false) String auth) {
        service.deletar(id, auth);
        return ResponseEntity.noContent().build();
    }
}
