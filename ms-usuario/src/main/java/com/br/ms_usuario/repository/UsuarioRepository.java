package com.br.ms_usuario.repository;

import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Usuario> findByParentesco(Parentesco parentesco);
}