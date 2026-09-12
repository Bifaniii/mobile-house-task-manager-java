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
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final long TOKEN_VALIDADE_MINUTOS = 30;

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioEventPublisher eventPublisher;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository repository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          UsuarioEventPublisher eventPublisher,
                          PasswordResetTokenRepository tokenRepository,
                          EmailService emailService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    public UsuarioResponseDTO toDTO(Usuario usuario) {
        if (usuario == null) return null;
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getParentesco()
        );
    }

    public List<UsuarioResponseDTO> getAllUsuarios() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UsuarioResponseDTO getUsuarioPorId(UUID id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        return toDTO(usuario);
    }

    public List<UsuarioResponseDTO> getUsuariosPorParentesco(Parentesco parentesco) {
        return repository.findByParentesco(parentesco).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void deletarUsuario(UUID id) {
        if (!repository.existsById(id)) {
            throw new UsuarioNaoEncontradoException(id);
        }
        repository.deleteById(id);
    }

    public UsuarioResponseDTO registrar(UsuarioRequestDTO dto) {
        if (repository.existsByEmail(dto.email())) {
            throw new EmailJaCadastradoException(dto.email());
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .parentesco(dto.parentesco())
                .build();

        usuario = repository.save(usuario);

        try {
            eventPublisher.publicarUsuarioCriado(
                    new UsuarioCriadoEvent(usuario.getId(), usuario.getNome(), usuario.getEmail())
            );
        } catch (Exception e) {
            log.warn("Não foi possível publicar evento de usuário criado: {}", e.getMessage());
        }

        return toDTO(usuario);
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = repository.findByEmail(dto.email())
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(dto.password(), usuario.getPassword())) {
            throw new CredenciaisInvalidasException();
        }

        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail());
        return new LoginResponseDTO(token, toDTO(usuario));
    }

    @Transactional
    public void esqueciSenha(EsqueciSenhaRequestDTO dto) {
        repository.findByEmail(dto.email()).ifPresent(usuario -> {
            tokenRepository.deleteByUsuarioId(usuario.getId());

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .usuarioId(usuario.getId())
                    .expiraEm(Instant.now().plus(TOKEN_VALIDADE_MINUTOS, ChronoUnit.MINUTES))
                    .usado(false)
                    .build();
            tokenRepository.save(resetToken);

            try {
                emailService.enviarEmailRedefinicaoSenha(usuario.getEmail(), resetToken.getToken());
            } catch (Exception e) {
                Throwable causa = e.getCause() != null ? e.getCause() : e;
                log.warn("Não foi possível enviar email de redefinição de senha: {}", causa.getMessage());
            }
        });
        // Resposta sempre igual, exista ou não o email, pra não vazar quais emails estão cadastrados
    }

    @Transactional
    public void redefinirSenha(RedefinirSenhaRequestDTO dto) {
        PasswordResetToken resetToken = tokenRepository.findByToken(dto.token())
                .filter(t -> !t.isUsado())
                .filter(t -> t.getExpiraEm().isAfter(Instant.now()))
                .orElseThrow(TokenInvalidoOuExpiradoException::new);

        Usuario usuario = repository.findById(resetToken.getUsuarioId())
                .orElseThrow(TokenInvalidoOuExpiradoException::new);

        usuario.setPassword(passwordEncoder.encode(dto.novaSenha()));
        repository.save(usuario);

        resetToken.setUsado(true);
        tokenRepository.save(resetToken);
    }
}