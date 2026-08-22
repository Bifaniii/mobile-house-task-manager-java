package com.br.ms_usuario.repository;

import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    public Usuario findByEmail(Usuario usuario);
    public List<Usuario> findByParentesco(Parentesco parentesco);
}
