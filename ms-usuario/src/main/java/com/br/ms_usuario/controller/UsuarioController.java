package com.br.ms_usuario.controller;

import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.dto.EsqueciSenhaRequestDTO;
import com.br.ms_usuario.domain.dto.LoginRequestDTO;
import com.br.ms_usuario.domain.dto.LoginResponseDTO;
import com.br.ms_usuario.domain.dto.RedefinirSenhaRequestDTO;
import com.br.ms_usuario.domain.dto.UsuarioRequestDTO;
import com.br.ms_usuario.domain.dto.UsuarioResponseDTO;
import com.br.ms_usuario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequestDTO dto) {
        service.esqueciSenha(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequestDTO dto) {
        service.redefinirSenha(dto);
        return ResponseEntity.ok().build();
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PAI', 'MAE')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}