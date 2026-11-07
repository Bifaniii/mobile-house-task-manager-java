package com.br.ms_carteira.service;

import com.br.ms_carteira.domain.Carteira;
import com.br.ms_carteira.domain.Lancamento;
import com.br.ms_carteira.repository.Repositorios.CarteiraRepository;
import com.br.ms_carteira.repository.Repositorios.LancamentoRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Único ponto que mexe em saldo: toda mudança grava junto a linha do extrato. */
@Service
public class CarteiraService {

    private final CarteiraRepository carteiras;
    private final LancamentoRepository lancamentos;

    public CarteiraService(CarteiraRepository carteiras, LancamentoRepository lancamentos) {
        this.carteiras = carteiras;
        this.lancamentos = lancamentos;
    }

    /** O broker pode entregar o mesmo evento mais de uma vez; a origem garante um crédito só. */
    @Transactional
    public void creditarTarefa(UUID eventoId, UUID usuarioId, UUID familiaId, int pontos, String titulo) {
        String origem = "tarefa:" + eventoId;
        if (lancamentos.existsByOrigem(origem)) return;

        movimentar(usuarioId, familiaId, Lancamento.Tipo.CREDITO_TAREFA, pontos, "Tarefa: " + titulo, origem);
    }

    /** Participa da transação de quem chamou. Saldo insuficiente responde 409 e nada é gravado. */
    public void movimentar(UUID usuarioId, UUID familiaId, Lancamento.Tipo tipo, int valor, String descricao, String origem) {
        Carteira carteira = carteiras.findById(usuarioId).orElseGet(() -> new Carteira(usuarioId, familiaId));
        int novoSaldo = carteira.getSaldo() + valor;
        if (novoSaldo < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pontos insuficientes para este prêmio");
        }
        carteira.setSaldo(novoSaldo);
        carteiras.save(carteira);
        lancamentos.save(new Lancamento(usuarioId, familiaId, tipo, valor, descricao, origem));
    }

    public int saldo(UUID usuarioId) {
        return carteiras.findById(usuarioId).map(Carteira::getSaldo).orElse(0);
    }

    public List<Lancamento> extrato(UUID usuarioId) {
        return lancamentos.findTop50ByUsuarioIdOrderByCriadoEmDesc(usuarioId);
    }

    public List<Carteira> daFamilia(UUID familiaId) {
        return carteiras.findByFamiliaId(familiaId);
    }
}
