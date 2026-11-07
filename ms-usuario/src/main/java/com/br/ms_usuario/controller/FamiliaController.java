package com.br.ms_usuario.controller;

import com.br.ms_usuario.domain.dto.FamiliaResponseDTO;
import com.br.ms_usuario.security.CustomUserDetails;
import com.br.ms_usuario.service.FamiliaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/familias/minha")
public class FamiliaController {

    private final FamiliaService service;

    public FamiliaController(FamiliaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<FamiliaResponseDTO> minha(@AuthenticationPrincipal CustomUserDetails logado) {
        return ResponseEntity.ok(service.minha(logado.getFamiliaId(), logado.getParentesco()));
    }

    @PostMapping("/codigo")
    @PreAuthorize("hasAnyRole('PAI', 'MAE')")
    public ResponseEntity<FamiliaResponseDTO> novoCodigo(@AuthenticationPrincipal CustomUserDetails logado) {
        return ResponseEntity.ok(service.gerarNovoCodigo(logado.getFamiliaId(), logado.getParentesco()));
    }
}
