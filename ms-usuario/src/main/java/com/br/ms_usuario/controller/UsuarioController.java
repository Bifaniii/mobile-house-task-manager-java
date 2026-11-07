package com.br.ms_usuario.controller;

import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.dto.EsqueciSenhaRequestDTO;
import com.br.ms_usuario.domain.dto.FilhoRequestDTO;
import com.br.ms_usuario.domain.dto.LoginRequestDTO;
import com.br.ms_usuario.domain.dto.LoginResponseDTO;
import com.br.ms_usuario.domain.dto.RedefinirSenhaRequestDTO;
import com.br.ms_usuario.domain.dto.UsuarioRequestDTO;
import com.br.ms_usuario.domain.dto.UsuarioResponseDTO;
import com.br.ms_usuario.security.CustomUserDetails;
import com.br.ms_usuario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /** Toda consulta autenticada fica limitada à família de quem está logado. */
    private UUID familiaLogada() {
        CustomUserDetails logado = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return logado.getFamiliaId();
    }

    @PostMapping("/registrar")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO response = service.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/filhos")
    @PreAuthorize("hasAnyRole('PAI', 'MAE')")
    public ResponseEntity<UsuarioResponseDTO> criarFilho(@Valid @RequestBody FilhoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarFilho(dto, familiaLogada()));
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
        return ResponseEntity.ok(service.getAllUsuarios(familiaLogada()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getUsuarioPorId(id, familiaLogada()));
    }

    @GetMapping("/parentesco/{parentesco}")
    public ResponseEntity<List<UsuarioResponseDTO>> listarPorParentesco(@PathVariable Parentesco parentesco) {
        return ResponseEntity.ok(service.getUsuariosPorParentesco(parentesco, familiaLogada()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PAI', 'MAE')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletarUsuario(id, familiaLogada());
        return ResponseEntity.noContent().build();
    }
}
