package com.br.ms_carteira.controller;

import com.br.ms_carteira.domain.Carteira;
import com.br.ms_carteira.domain.Lancamento;
import com.br.ms_carteira.domain.Premio;
import com.br.ms_carteira.domain.Resgate;
import com.br.ms_carteira.repository.Repositorios.PremioRepository;
import com.br.ms_carteira.security.JwtService;
import com.br.ms_carteira.security.JwtService.Sessao;
import com.br.ms_carteira.service.CarteiraService;
import com.br.ms_carteira.service.ResgateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class CarteiraController {

    public record SaldoDTO(UUID usuarioId, int saldo) { }

    public record LancamentoDTO(UUID id, Lancamento.Tipo tipo, int valor, String descricao, Instant criadoEm) { }

    public record MinhaCarteiraDTO(int saldo, List<LancamentoDTO> lancamentos) { }

    public record PremioRequestDTO(
            @NotBlank(message = "Dê um nome para o prêmio")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres") String nome,
            @NotNull(message = "Informe quantos pontos o prêmio custa")
            @Min(value = 1, message = "O prêmio custa pelo menos 1 ponto")
            @Max(value = 100000, message = "O prêmio custa no máximo 100000 pontos") Integer custo) { }

    public record PremioDTO(UUID id, String nome, int custo) { }

    public record ResgateRequestDTO(@NotNull(message = "Escolha um prêmio") UUID premioId) { }

    public record ResgateDTO(UUID id, UUID usuarioId, String premioNome, int custo, Resgate.Status status, Instant criadoEm) { }

    private final JwtService jwt;
    private final CarteiraService carteira;
    private final ResgateService resgates;
    private final PremioRepository premios;

    public CarteiraController(JwtService jwt, CarteiraService carteira, ResgateService resgates, PremioRepository premios) {
        this.jwt = jwt;
        this.carteira = carteira;
        this.resgates = resgates;
        this.premios = premios;
    }

    @GetMapping("/carteiras/minha")
    public MinhaCarteiraDTO minha(@RequestHeader(value = "Authorization", required = false) String auth) {
        UUID eu = jwt.sessao(auth).usuarioId();
        List<LancamentoDTO> extrato = carteira.extrato(eu).stream()
                .map(l -> new LancamentoDTO(l.getId(), l.getTipo(), l.getValor(), l.getDescricao(), l.getCriadoEm()))
                .toList();
        return new MinhaCarteiraDTO(carteira.saldo(eu), extrato);
    }

    @GetMapping("/carteiras")
    public List<SaldoDTO> daFamilia(@RequestHeader(value = "Authorization", required = false) String auth) {
        List<Carteira> lista = carteira.daFamilia(jwt.sessao(auth).familiaId());
        return lista.stream().map(c -> new SaldoDTO(c.getUsuarioId(), c.getSaldo())).toList();
    }

    @GetMapping("/premios")
    public List<PremioDTO> listarPremios(@RequestHeader(value = "Authorization", required = false) String auth) {
        return premios.findByFamiliaIdAndAtivoTrueOrderByCustoAsc(jwt.sessao(auth).familiaId()).stream()
                .map(CarteiraController::toDTO)
                .toList();
    }

    @PostMapping("/premios")
    public ResponseEntity<PremioDTO> criarPremio(@Valid @RequestBody PremioRequestDTO dto,
                                                 @RequestHeader(value = "Authorization", required = false) String auth) {
        Sessao sessao = jwt.exigirPaiOuMae(auth);
        Premio premio = new Premio();
        premio.setFamiliaId(sessao.familiaId());
        premio.setNome(dto.nome().trim());
        premio.setCusto(dto.custo());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(premios.save(premio)));
    }

    @DeleteMapping("/premios/{id}")
    public ResponseEntity<Void> removerPremio(@PathVariable UUID id,
                                              @RequestHeader(value = "Authorization", required = false) String auth) {
        Sessao sessao = jwt.exigirPaiOuMae(auth);
        Premio premio = premios.findById(id)
                .filter(p -> sessao.familiaId().equals(p.getFamiliaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prêmio não encontrado"));
        premio.setAtivo(false);
        premios.save(premio);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resgates")
    public List<ResgateDTO> listarResgates(@RequestHeader(value = "Authorization", required = false) String auth) {
        return resgates.listar(jwt.sessao(auth)).stream().map(CarteiraController::toDTO).toList();
    }

    @PostMapping("/resgates")
    public ResponseEntity<ResgateDTO> pedirResgate(@Valid @RequestBody ResgateRequestDTO dto,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(resgates.pedir(dto.premioId(), jwt.sessao(auth))));
    }

    @PatchMapping("/resgates/{id}/aprovar")
    public ResgateDTO aprovarResgate(@PathVariable UUID id,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        return toDTO(resgates.aprovar(id, jwt.exigirPaiOuMae(auth)));
    }

    @PatchMapping("/resgates/{id}/recusar")
    public ResgateDTO recusarResgate(@PathVariable UUID id,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        return toDTO(resgates.recusar(id, jwt.exigirPaiOuMae(auth)));
    }

    private static PremioDTO toDTO(Premio p) {
        return new PremioDTO(p.getId(), p.getNome(), p.getCusto());
    }

    private static ResgateDTO toDTO(Resgate r) {
        return new ResgateDTO(r.getId(), r.getUsuarioId(), r.getPremioNome(), r.getCusto(), r.getStatus(), r.getCriadoEm());
    }
}
