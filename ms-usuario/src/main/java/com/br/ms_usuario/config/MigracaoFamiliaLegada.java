package com.br.ms_usuario.config;

import com.br.ms_usuario.domain.Familia;
import com.br.ms_usuario.domain.Usuario;
import com.br.ms_usuario.repository.UsuarioRepository;
import com.br.ms_usuario.service.FamiliaService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Antes das famílias, todas as contas enxergavam umas às outras. Reunir as contas antigas em uma única
 * família preserva esse comportamento para elas, sem perder dados nem exigir novo cadastro.
 */
@Component
public class MigracaoFamiliaLegada implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoFamiliaLegada.class);

    private final UsuarioRepository usuarios;
    private final FamiliaService familias;

    public MigracaoFamiliaLegada(UsuarioRepository usuarios, FamiliaService familias) {
        this.usuarios = usuarios;
        this.familias = familias;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Usuario> semFamilia = usuarios.findByFamiliaIdIsNull();
        if (semFamilia.isEmpty()) return;

        familias.garantirFamiliaLegada();
        semFamilia.forEach(usuario -> usuario.setFamiliaId(Familia.ID_LEGADO));
        usuarios.saveAll(semFamilia);
        log.info("{} contas antigas movidas para a família legada", semFamilia.size());
    }
}
