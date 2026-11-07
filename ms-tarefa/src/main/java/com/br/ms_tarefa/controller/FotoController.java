package com.br.ms_tarefa.controller;

import com.br.ms_tarefa.domain.FotoTarefa;
import com.br.ms_tarefa.service.FotoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tarefas/{id}/fotos")
public class FotoController {

    private final FotoService service;

    public FotoController(FotoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<FotoTarefa.Tipo>> listar(@PathVariable UUID id,
                                                        @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(service.listar(id, auth));
    }

    @PostMapping("/{tipo}")
    public ResponseEntity<Void> enviar(@PathVariable UUID id,
                                       @PathVariable FotoTarefa.Tipo tipo,
                                       @RequestParam("arquivo") MultipartFile arquivo,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        service.salvar(id, tipo, arquivo, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tipo}")
    public ResponseEntity<byte[]> abrir(@PathVariable UUID id,
                                        @PathVariable FotoTarefa.Tipo tipo,
                                        @RequestHeader(value = "Authorization", required = false) String auth) {
        FotoTarefa foto = service.abrir(id, tipo, auth);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.getConteudo());
    }
}
