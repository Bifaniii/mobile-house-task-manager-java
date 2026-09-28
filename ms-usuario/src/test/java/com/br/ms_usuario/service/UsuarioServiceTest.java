package com.br.ms_usuario.service;

import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.PasswordResetToken;
import com.br.ms_usuario.domain.Usuario;
import com.br.ms_usuario.domain.dto.*;
import com.br.ms_usuario.exceptions.CredenciaisInvalidasException;
import com.br.ms_usuario.exceptions.EmailJaCadastradoException;
import com.br.ms_usuario.exceptions.TokenInvalidoOuExpiradoException;
import com.br.ms_usuario.exceptions.UsuarioNaoEncontradoException;
import com.br.ms_usuario.mail.EmailService;
import com.br.ms_usuario.messaging.UsuarioCriadoEvent;
import com.br.ms_usuario.messaging.UsuarioEventPublisher;
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

    @InjectMocks
    private UsuarioService service;

    private final UUID id = UUID.randomUUID();

    private Usuario usuario() {
        return Usuario.builder()
                .id(id)
                .nome("Ana")
                .email("ana@casa.com")
                .password("hash")
                .parentesco(Parentesco.MAE)
                .build();
    }

    private UsuarioRequestDTO cadastro() {
        return new UsuarioRequestDTO("Ana", "ana@casa.com", "123456", Parentesco.MAE);
    }

    @Test
    void registrar_deveSalvarComSenhaCriptografadaEPublicarEvento() {
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
        when(jwtService.generateToken(id, "ana@casa.com")).thenReturn("jwt");

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
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deletarUsuario(id)).isInstanceOf(UsuarioNaoEncontradoException.class);
        verify(repository, never()).deleteById(any());
    }
}
