package com.br.ms_carteira;

import com.br.ms_carteira.domain.Lancamento;
import com.br.ms_carteira.domain.Premio;
import com.br.ms_carteira.domain.Resgate;
import com.br.ms_carteira.messaging.TarefaAprovadaListener;
import com.br.ms_carteira.repository.Repositorios.PremioRepository;
import com.br.ms_carteira.security.JwtService.Sessao;
import com.br.ms_carteira.service.CarteiraService;
import com.br.ms_carteira.service.ResgateService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Roda com banco real (H2): o que importa aqui é a transação, o @Version e a unicidade do extrato. */
@SpringBootTest
class MsCarteiraIntegracaoTest {

    @Autowired
    private CarteiraService carteira;
    @Autowired
    private ResgateService resgates;
    @Autowired
    private PremioRepository premios;
    @Autowired
    private TarefaAprovadaListener listener;

    private final UUID familia = UUID.randomUUID();
    private final UUID filha = UUID.randomUUID();
    private final Sessao sessaoFilha = new Sessao(filha, familia, "FILHO");
    private final Sessao sessaoMae = new Sessao(UUID.randomUUID(), familia, "MAE");

    private Premio premio(int custo) {
        Premio premio = new Premio();
        premio.setFamiliaId(familia);
        premio.setNome("Pizza");
        premio.setCusto(custo);
        return premios.save(premio);
    }

    private void entregar(UUID eventoId, int pontos) {
        String json = """
                {"eventoId":"%s","tarefaId":"%s","usuarioId":"%s","familiaId":"%s","pontos":%d,"titulo":"Lavar a louça"}
                """.formatted(eventoId, UUID.randomUUID(), filha, familia, pontos);
        listener.aoAprovar(MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build());
    }

    @Test
    void eventoRepetidoCreditaUmaVezSo() {
        UUID evento = UUID.randomUUID();

        entregar(evento, 5);
        entregar(evento, 5);

        assertThat(carteira.saldo(filha)).isEqualTo(5);
        assertThat(carteira.extrato(filha)).hasSize(1);
        assertThat(carteira.extrato(filha).get(0).getDescricao()).isEqualTo("Tarefa: Lavar a louça");
    }

    @Test
    void mensagemMalformadaEDescartadaSemTocarNoSaldo() {
        assertThatThrownBy(() -> listener.aoAprovar(MessageBuilder.withBody("nao e json".getBytes()).build()))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThat(carteira.saldo(filha)).isZero();
    }

    @Test
    void resgateSemSaldoRecebe409ENaoGravaNada() {
        entregar(UUID.randomUUID(), 3);
        UUID premioId = premio(10).getId();

        assertThatThrownBy(() -> resgates.pedir(premioId, sessaoFilha))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(carteira.saldo(filha)).isEqualTo(3);
        assertThat(resgates.listar(sessaoFilha)).isEmpty();
    }

    @Test
    void resgateDebitaNaHoraERecusaDevolveUmaVezSo() {
        entregar(UUID.randomUUID(), 10);
        Resgate resgate = resgates.pedir(premio(8).getId(), sessaoFilha);
        assertThat(carteira.saldo(filha)).isEqualTo(2);

        resgates.recusar(resgate.getId(), sessaoMae);
        assertThat(carteira.saldo(filha)).isEqualTo(10);

        assertThatThrownBy(() -> resgates.recusar(resgate.getId(), sessaoMae)).isInstanceOf(ResponseStatusException.class);
        assertThat(carteira.saldo(filha)).isEqualTo(10);
        assertThat(carteira.extrato(filha)).extracting(Lancamento::getTipo)
                .containsExactlyInAnyOrder(Lancamento.Tipo.CREDITO_TAREFA, Lancamento.Tipo.RESGATE, Lancamento.Tipo.ESTORNO);
    }

    @Test
    void resgateAprovadoNaoDevolvePontosENaoPodeSerRecusadoDepois() {
        entregar(UUID.randomUUID(), 10);
        Resgate resgate = resgates.pedir(premio(4).getId(), sessaoFilha);

        assertThat(resgates.aprovar(resgate.getId(), sessaoMae).getStatus()).isEqualTo(Resgate.Status.APROVADO);

        assertThatThrownBy(() -> resgates.recusar(resgate.getId(), sessaoMae)).isInstanceOf(ResponseStatusException.class);
        assertThat(carteira.saldo(filha)).isEqualTo(6);
    }

    @Test
    void naoEnxergaPremioNemDecideResgateDeOutraFamilia() {
        entregar(UUID.randomUUID(), 10);
        Premio premio = premio(1);
        Sessao deFora = new Sessao(UUID.randomUUID(), UUID.randomUUID(), "MAE");

        assertThatThrownBy(() -> resgates.pedir(premio.getId(), deFora)).isInstanceOf(ResponseStatusException.class);

        Resgate resgate = resgates.pedir(premio.getId(), sessaoFilha);
        assertThatThrownBy(() -> resgates.aprovar(resgate.getId(), deFora))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
