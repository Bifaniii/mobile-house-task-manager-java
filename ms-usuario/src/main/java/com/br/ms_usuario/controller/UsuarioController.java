package com.br.ms_usuario.controller;

import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.dto.LoginRequestDTO;
import com.br.ms_usuario.domain.dto.LoginResponseDTO;
import com.br.ms_usuario.domain.dto.UsuarioRequestDTO;
import com.br.ms_usuario.domain.dto.UsuarioResponseDTO;
import com.br.ms_usuario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/registrar")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO response = service.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(service.login(dto));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(service.getAllUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getUsuarioPorId(id));
    }

    @GetMapping("/parentesco/{parentesco}")
    public ResponseEntity<List<UsuarioResponseDTO>> listarPorParentesco(@PathVariable Parentesco parentesco) {
        return ResponseEntity.ok(service.getUsuariosPorParentesco(parentesco));
    }
}