package com.br.ms_usuario.service;

import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.Usuario;
import com.br.ms_usuario.domain.dto.UsuarioResponseDTO;
import com.br.ms_usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {
    public final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public UsuarioResponseDTO toDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getParentesco()
        );
    }

    public List<UsuarioResponseDTO> getAllUsers() {
        List<Usuario> listaUsuarios = repository.findAll();

        return listaUsuarios.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UsuarioResponseDTO createUsuario(Usuario usuario) {
        repository.save(usuario);
        return toDTO(usuario);
    }
}
