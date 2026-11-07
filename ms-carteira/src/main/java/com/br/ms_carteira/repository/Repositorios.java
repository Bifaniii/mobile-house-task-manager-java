package com.br.ms_carteira.repository;

import com.br.ms_carteira.domain.Carteira;
import com.br.ms_carteira.domain.Lancamento;
import com.br.ms_carteira.domain.Premio;
import com.br.ms_carteira.domain.Resgate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public final class Repositorios {

    private Repositorios() {
    }

    public interface CarteiraRepository extends JpaRepository<Carteira, UUID> {
        List<Carteira> findByFamiliaId(UUID familiaId);
    }

    public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {
        boolean existsByOrigem(String origem);

        List<Lancamento> findTop50ByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);
    }

    public interface PremioRepository extends JpaRepository<Premio, UUID> {
        List<Premio> findByFamiliaIdAndAtivoTrueOrderByCustoAsc(UUID familiaId);
    }

    public interface ResgateRepository extends JpaRepository<Resgate, UUID> {
        List<Resgate> findByFamiliaIdOrderByCriadoEmDesc(UUID familiaId);

        List<Resgate> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);
    }
}
