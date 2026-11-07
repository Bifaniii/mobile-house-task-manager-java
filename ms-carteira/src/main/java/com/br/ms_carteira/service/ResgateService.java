package com.br.ms_carteira.service;

import com.br.ms_carteira.domain.Lancamento;
import com.br.ms_carteira.domain.Premio;
import com.br.ms_carteira.domain.Resgate;
import com.br.ms_carteira.repository.Repositorios.PremioRepository;
import com.br.ms_carteira.repository.Repositorios.ResgateRepository;
import com.br.ms_carteira.security.JwtService.Sessao;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ResgateService {

    private final ResgateRepository resgates;
    private final PremioRepository premios;
    private final CarteiraService carteira;

    public ResgateService(ResgateRepository resgates, PremioRepository premios, CarteiraService carteira) {
        this.resgates = resgates;
        this.premios = premios;
        this.carteira = carteira;
    }

    /** Os pontos saem na hora do pedido, para não serem gastos de novo enquanto ninguém decide. */
    @Transactional
    public Resgate pedir(UUID premioId, Sessao sessao) {
        Premio premio = premios.findById(premioId)
                .filter(p -> p.isAtivo() && sessao.familiaId().equals(p.getFamiliaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prêmio não encontrado"));

        Resgate resgate = new Resgate();
        resgate.setId(UUID.randomUUID());
        resgate.setFamiliaId(sessao.familiaId());
        resgate.setUsuarioId(sessao.usuarioId());
        resgate.setPremioId(premio.getId());
        resgate.setPremioNome(premio.getNome());
        resgate.setCusto(premio.getCusto());

        carteira.movimentar(sessao.usuarioId(), sessao.familiaId(), Lancamento.Tipo.RESGATE, -premio.getCusto(),
                "Resgate: " + premio.getNome(), "resgate:" + resgate.getId());
        return resgates.save(resgate);
    }

    @Transactional
    public Resgate aprovar(UUID id, Sessao decisor) {
        Resgate resgate = buscarPendente(id, decisor);
        resgate.setStatus(Resgate.Status.APROVADO);
        resgate.setDecididoPor(decisor.usuarioId());
        return resgates.save(resgate);
    }

    @Transactional
    public Resgate recusar(UUID id, Sessao decisor) {
        Resgate resgate = buscarPendente(id, decisor);
        resgate.setStatus(Resgate.Status.RECUSADO);
        resgate.setDecididoPor(decisor.usuarioId());
        carteira.movimentar(resgate.getUsuarioId(), resgate.getFamiliaId(), Lancamento.Tipo.ESTORNO, resgate.getCusto(),
                "Devolução: " + resgate.getPremioNome(), "estorno:" + resgate.getId());
        return resgates.save(resgate);
    }

    /** Mãe ou pai vê os pedidos da família; filhos veem só os próprios. */
    public List<Resgate> listar(Sessao sessao) {
        return sessao.ehPaiOuMae()
                ? resgates.findByFamiliaIdOrderByCriadoEmDesc(sessao.familiaId())
                : resgates.findByUsuarioIdOrderByCriadoEmDesc(sessao.usuarioId());
    }

    private Resgate buscarPendente(UUID id, Sessao decisor) {
        Resgate resgate = resgates.findById(id)
                .filter(r -> decisor.familiaId().equals(r.getFamiliaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resgate não encontrado"));
        if (resgate.getStatus() != Resgate.Status.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este resgate já foi decidido");
        }
        return resgate;
    }
}
