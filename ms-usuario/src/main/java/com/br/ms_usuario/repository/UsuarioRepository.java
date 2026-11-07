package com.br.ms_usuario.repository;

import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Usuario> findByIdAndFamiliaId(UUID id, UUID familiaId);
    List<Usuario> findByFamiliaId(UUID familiaId);
    List<Usuario> findByFamiliaIdAndParentesco(UUID familiaId, Parentesco parentesco);
    List<Usuario> findByFamiliaIdIsNull();
    long countByFamiliaId(UUID familiaId);
    long countByFamiliaIdAndParentescoIn(UUID familiaId, Collection<Parentesco> parentescos);
}
