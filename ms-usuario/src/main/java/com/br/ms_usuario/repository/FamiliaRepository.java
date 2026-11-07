package com.br.ms_usuario.repository;

import com.br.ms_usuario.domain.Familia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FamiliaRepository extends JpaRepository<Familia, UUID> {

    Optional<Familia> findByCodigoConvite(String codigoConvite);

    boolean existsByCodigoConvite(String codigoConvite);
}
