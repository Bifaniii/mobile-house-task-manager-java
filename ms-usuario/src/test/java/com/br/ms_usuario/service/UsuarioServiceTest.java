package com.br.ms_usuario.service;

import com.br.ms_usuario.domain.Familia;
import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.PasswordResetToken;
import com.br.ms_usuario.domain.Usuario;
import com.br.ms_usuario.domain.dto.*;
import com.br.ms_usuario.exceptions.CredenciaisInvalidasException;
import com.br.ms_usuario.exceptions.EmailJaCadastradoException;
import com.br.ms_usuario.exceptions.RegraDeNegocioException;
import com.br.ms_usuario.exceptions.TokenInvalidoOuExpiradoException;
import com.br.ms_usuario.exceptions.UsuarioNaoEncontradoException;
import com.br.ms_usuario.mail.EmailService;
import com.br.ms_usuario.messaging.UsuarioCriadoEvent;
import com.br.ms_usuario.messaging.UsuarioEventPublisher;
import com.br.ms_usuario.messaging.UsuarioRemovidoEvent;
import com.br.ms_usuario.repository.PasswordResetTokenRepository;
import com.br.ms_usuario.repository.UsuarioRepository;
import com.br.ms_usuario.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioEventPublisher eventPublisher;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private FamiliaService familiaService;

    @InjectMocks
    private UsuarioService service;

    private final UUID id = UUID.randomUUID();
    private final UUID familiaId = UUID.randomUUID();
    private final Familia familia = Familia.builder().id(familiaId).nome("Souza").codigoConvite("ABCD2345").build();

    private Usuario usuario() {
        return Usuario.builder()
                .id(id)
                .nome("Ana")
                .email("ana@casa.com")
                .password("hash")
                .parentesco(Parentesco.MAE)
                .familiaId(familiaId)
                .build();
    }

    private UsuarioRequestDTO cadastro() {
        return new UsuarioRequestDTO("Ana", "ana@casa.com", "123456", Parentesco.MAE, "Souza", null);
    }

    private static void assertStatus(Runnable acao, HttpStatus esperado) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting(e -> ((RegraDeNegocioException) e).getStatus())
                .isEqualTo(esperado);
    }

    @Test
    void registrar_filhoNaoPodeSeCadastrarSozinho() {
        assertStatus(() -> service.registrar(new UsuarioRequestDTO("Bia", "bia@casa.com", "123456", Parentesco.FILHO, "Souza", null)),
                HttpStatus.FORBIDDEN);
        verify(repository, never()).save(any());
    }

    @Test
    void registrar_exigeNomeDaFamiliaOuConvite() {
        assertStatus(() -> service.registrar(new UsuarioRequestDTO("Ana", "ana@casa.com", "123456", Parentesco.MAE, " ", null)),
                HttpStatus.BAD_REQUEST);
        verify(repository, never()).save(any());
    }

    @Test
    void registrar_comConviteEntraNaFamiliaExistente() {
        when(familiaService.buscarPorConvite("ABCD2345")).thenReturn(familia);
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrar(new UsuarioRequestDTO("Rui", "rui@casa.com", "123456", Parentesco.PAI, null, "ABCD2345"));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getFamiliaId()).isEqualTo(familiaId);
        verify(familiaService, never()).criar(any());
    }

    @Test
    void criarFilho_nasceNaFamiliaDeQuemCriou() {
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponseDTO filho = service.criarFilho(new FilhoRequestDTO("Bia", "bia@casa.com", "123456"), familiaId);

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getFamiliaId()).isEqualTo(familiaId);
        assertThat(filho.parentesco()).isEqualTo(Parentesco.FILHO);
    }

    @Test
    void getUsuarioPorId_naoEnxergaPessoaDeOutraFamilia() {
        UUID outra = UUID.randomUUID();
        when(repository.findByIdAndFamiliaId(id, outra)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUsuarioPorId(id, outra)).isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    void deletarUsuario_ultimoResponsavelNaoPodeSairDeixandoFilhos() {
        when(repository.findByIdAndFamiliaId(id, familiaId)).thenReturn(Optional.of(usuario()));
        when(repository.countByFamiliaIdAndParentescoIn(any(), any())).thenReturn(1L);
        when(repository.countByFamiliaId(familiaId)).thenReturn(2L);

        assertStatus(() -> service.deletarUsuario(id, familiaId), HttpStatus.CONFLICT);
        verify(repository, never()).delete(any());
    }

    @Test
    void deletarUsuario_publicaEventoEToleraRabbitForaDoAr() {
        Usuario filho = usuario();
        filho.setParentesco(Parentesco.FILHO);
        when(repository.findByIdAndFamiliaId(id, familiaId)).thenReturn(Optional.of(filho));
        doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .when(eventPublisher).publicarUsuarioRemovido(any());

        service.deletarUsuario(id, familiaId);

        verify(repository).delete(filho);
        verify(eventPublisher).publicarUsuarioRemovido(new UsuarioRemovidoEvent(id, familiaId));
    }

    @Test
    void registrar_deveSalvarComSenhaCriptografadaEPublicarEvento() {
        when(familiaService.criar("Souza")).thenReturn(familia);
        when(repository.existsByEmail("ana@casa.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash-bcrypt");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(id);
            return u;
        });

        UsuarioResponseDTO response = service.registrar(cadastro());

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getPassword()).isEqualTo("hash-bcrypt");
        verify(eventPublisher).publicarUsuarioCriado(new UsuarioCriadoEvent(id, "Ana", "ana@casa.com"));
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.parentesco()).isEqualTo(Parentesco.MAE);
    }

    @Test
    void registrar_deveRejeitarEmailJaCadastrado() {
        when(repository.existsByEmail("ana@casa.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(cadastro()))
                .isInstanceOf(EmailJaCadastradoException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void registrar_naoDeveFalharQuandoRabbitMQEstaFora() {
        when(familiaService.criar("Souza")).thenReturn(familia);
        when(repository.existsByEmail("ana@casa.com")).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenReturn(usuario());
        doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .when(eventPublisher).publicarUsuarioCriado(any());

        assertThat(service.registrar(cadastro()).email()).isEqualTo("ana@casa.com");
    }

    @Test
    void login_deveRetornarTokenBearerQuandoSenhaConfere() {
        when(repository.findByEmail("ana@casa.com")).thenReturn(Optional.of(usuario()));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(jwtService.generateToken(id, "ana@casa.com", familiaId, Parentesco.MAE)).thenReturn("jwt");

        LoginResponseDTO response = service.login(new LoginRequestDTO("ana@casa.com", "123456"));

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.tipo()).isEqualTo("Bearer");
    }

    @Test
    void login_deveRejeitarSenhaErrada() {
        when(repository.findByEmail("ana@casa.com")).thenReturn(Optional.of(usuario()));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequestDTO("ana@casa.com", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_deveDarMesmoErroQuandoEmailNaoExiste() {
        when(repository.findByEmail("x@casa.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequestDTO("x@casa.com", "123456")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Email ou senha inválidos");
    }

    @Test
    void esqueciSenha_deveSubstituirTokenAnteriorEEnviarEmail() {
        when(repository.findByEmail("ana@casa.com")).thenReturn(Optional.of(usuario()));

        service.esqueciSenha(new EsqueciSenhaRequestDTO("ana@casa.com"));

        verify(tokenRepository).deleteByUsuarioId(id);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken token = captor.getValue();
        assertThat(token.isUsado()).isFalse();
        assertThat(token.getExpiraEm()).isBetween(Instant.now().plus(29, ChronoUnit.MINUTES), Instant.now().plus(31, ChronoUnit.MINUTES));
        verify(emailService).enviarEmailRedefinicaoSenha("ana@casa.com", token.getToken());
    }

    @Test
    void esqueciSenha_naoDeveRevelarQueEmailNaoExiste() {
        when(repository.findByEmail("x@casa.com")).thenReturn(Optional.empty());

        service.esqueciSenha(new EsqueciSenhaRequestDTO("x@casa.com"));

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void esqueciSenha_naoDevePropagarFalhaNoEnvioDeEmail() {
        when(repository.findByEmail("ana@casa.com")).thenReturn(Optional.of(usuario()));
        doThrow(new MailSendException("smtp fora")).when(emailService).enviarEmailRedefinicaoSenha(anyString(), anyString());

        service.esqueciSenha(new EsqueciSenhaRequestDTO("ana@casa.com"));

        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void redefinirSenha_deveTrocarSenhaEMarcarTokenComoUsado() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("abc").usuarioId(id).expiraEm(Instant.now().plusSeconds(600)).usado(false).build();
        Usuario usuario = usuario();
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));
        when(repository.findById(id)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha")).thenReturn("novo-hash");

        service.redefinirSenha(new RedefinirSenhaRequestDTO("abc", "novaSenha"));

        assertThat(usuario.getPassword()).isEqualTo("novo-hash");
        assertThat(token.isUsado()).isTrue();
        verify(tokenRepository).save(token);
    }

    @Test
    void redefinirSenha_deveRejeitarTokenJaUsado() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("abc").usuarioId(id).expiraEm(Instant.now().plusSeconds(600)).usado(true).build();
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.redefinirSenha(new RedefinirSenhaRequestDTO("abc", "novaSenha")))
                .isInstanceOf(TokenInvalidoOuExpiradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void redefinirSenha_deveRejeitarTokenExpirado() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("abc").usuarioId(id).expiraEm(Instant.now().minusSeconds(1)).usado(false).build();
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.redefinirSenha(new RedefinirSenhaRequestDTO("abc", "novaSenha")))
                .isInstanceOf(TokenInvalidoOuExpiradoException.class);
    }

    @Test
    void deletarUsuario_deveFalharQuandoNaoExiste() {
        when(repository.findByIdAndFamiliaId(id, familiaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletarUsuario(id, familiaId)).isInstanceOf(UsuarioNaoEncontradoException.class);
        verify(repository, never()).delete(any());
    }
}
