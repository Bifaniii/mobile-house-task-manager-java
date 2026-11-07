package com.br.ms_tarefa.service;

import com.br.ms_tarefa.domain.FotoTarefa;
import com.br.ms_tarefa.domain.Status;
import com.br.ms_tarefa.domain.Tarefa;
import com.br.ms_tarefa.repository.FotoTarefaRepository;
import com.br.ms_tarefa.repository.TarefaRepository;
import com.br.ms_tarefa.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FotoServiceTest {

    private static final String AUTH = "Bearer token";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    @Mock
    private FotoTarefaRepository repository;
    @Mock
    private TarefaRepository tarefaRepository;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private FotoService service;

    private final UUID familia = UUID.randomUUID();
    private final UUID filho = UUID.randomUUID();
    private final Tarefa tarefa = new Tarefa();

    @BeforeEach
    void setUp() {
        tarefa.setId(UUID.randomUUID());
        tarefa.setFamiliaId(familia);
        tarefa.setUsuarioId(filho);
        tarefa.setStatus(Status.EM_ANDAMENTO);
        when(jwtService.familiaDe(AUTH)).thenReturn(familia);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        lenient().when(jwtService.autenticado(AUTH)).thenReturn(filho);
    }

    private static MockMultipartFile arquivo(byte[] bytes, String tipoDeclarado) {
        return new MockMultipartFile("arquivo", "foto", tipoDeclarado, bytes);
    }

    private void assertStatus(Runnable acao, HttpStatus esperado) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(esperado);
        verify(repository, never()).save(any());
    }

    @Test
    void salvar_guardaJpegComTipoDetectadoPelosBytes() {
        when(repository.findByTarefaIdAndTipo(tarefa.getId(), FotoTarefa.Tipo.DEPOIS)).thenReturn(Optional.empty());

        service.salvar(tarefa.getId(), FotoTarefa.Tipo.DEPOIS, arquivo(JPEG, "text/html"), AUTH);

        ArgumentCaptor<FotoTarefa> salva = ArgumentCaptor.forClass(FotoTarefa.class);
        verify(repository).save(salva.capture());
        assertThat(salva.getValue().getContentType()).isEqualTo("image/jpeg");
        assertThat(salva.getValue().getConteudo()).isEqualTo(JPEG);
    }

    @Test
    void salvar_recusaArquivoQueNaoEImagemMesmoDeclarandoSerJpeg() {
        byte[] html = "<script>alert(1)</script>".getBytes();

        assertStatus(() -> service.salvar(tarefa.getId(), FotoTarefa.Tipo.DEPOIS, arquivo(html, "image/jpeg"), AUTH),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void salvar_recusaFotoMaiorQueOLimite() {
        byte[] grande = new byte[FotoService.TAMANHO_MAXIMO + 1];

        assertStatus(() -> service.salvar(tarefa.getId(), FotoTarefa.Tipo.DEPOIS, arquivo(grande, "image/jpeg"), AUTH),
                HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void salvar_soQuemFazATarefaPodeEnviar() {
        when(jwtService.autenticado(AUTH)).thenReturn(UUID.randomUUID());

        assertStatus(() -> service.salvar(tarefa.getId(), FotoTarefa.Tipo.DEPOIS, arquivo(JPEG, "image/jpeg"), AUTH),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void salvar_tarefaConcluidaNaoAceitaFoto() {
        tarefa.setStatus(Status.CONCLUIDA);

        assertStatus(() -> service.salvar(tarefa.getId(), FotoTarefa.Tipo.DEPOIS, arquivo(JPEG, "image/jpeg"), AUTH),
                HttpStatus.CONFLICT);
    }

    @Test
    void abrir_fotoDeTarefaDeOutraFamiliaRecebe404() {
        tarefa.setFamiliaId(UUID.randomUUID());

        assertStatus(() -> service.abrir(tarefa.getId(), FotoTarefa.Tipo.DEPOIS, AUTH), HttpStatus.NOT_FOUND);
    }
}
