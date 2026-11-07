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
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    private final FamiliaService familiaService;

    public UsuarioService(UsuarioRepository repository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          UsuarioEventPublisher eventPublisher,
                          PasswordResetTokenRepository tokenRepository,
                          EmailService emailService,
                          FamiliaService familiaService) {
        this.familiaService = familiaService;
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

    public List<UsuarioResponseDTO> getAllUsuarios(UUID familiaId) {
        return repository.findByFamiliaId(familiaId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UsuarioResponseDTO getUsuarioPorId(UUID id, UUID familiaId) {
        return toDTO(buscarNaFamilia(id, familiaId));
    }

    public List<UsuarioResponseDTO> getUsuariosPorParentesco(Parentesco parentesco, UUID familiaId) {
        return repository.findByFamiliaIdAndParentesco(familiaId, parentesco).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletarUsuario(UUID id, UUID familiaId) {
        Usuario usuario = buscarNaFamilia(id, familiaId);

        // Sem mãe ou pai, ninguém mais poderia aprovar tarefas nem gerenciar a família.
        boolean ehResponsavel = usuario.getParentesco() != Parentesco.FILHO;
        if (ehResponsavel
                && repository.countByFamiliaIdAndParentescoIn(familiaId, List.of(Parentesco.MAE, Parentesco.PAI)) == 1
                && repository.countByFamiliaId(familiaId) > 1) {
            throw new RegraDeNegocioException(HttpStatus.CONFLICT,
                    "A família precisa de pelo menos uma mãe ou um pai enquanto tiver filhos.");
        }

        repository.delete(usuario);

        try {
            eventPublisher.publicarUsuarioRemovido(new UsuarioRemovidoEvent(id, familiaId));
        } catch (Exception e) {
            log.warn("Não foi possível publicar evento de usuário removido: {}", e.getMessage());
        }
    }

    @Transactional
    public UsuarioResponseDTO registrar(UsuarioRequestDTO dto) {
        if (dto.parentesco() == Parentesco.FILHO) {
            throw new RegraDeNegocioException(HttpStatus.FORBIDDEN,
                    "A conta de filho ou filha é criada pela mãe ou pelo pai, dentro do app.");
        }
        boolean temConvite = dto.codigoConvite() != null && !dto.codigoConvite().isBlank();
        if (!temConvite && (dto.nomeFamilia() == null || dto.nomeFamilia().isBlank())) {
            throw new RegraDeNegocioException(HttpStatus.BAD_REQUEST,
                    "Informe o nome da família ou um código de convite.");
        }
        exigirEmailLivre(dto.email());

        Familia familia = temConvite
                ? familiaService.buscarPorConvite(dto.codigoConvite())
                : familiaService.criar(dto.nomeFamilia());

        return salvar(dto.nome(), dto.email(), dto.password(), dto.parentesco(), familia.getId());
    }

    /** Só mãe ou pai chega aqui (ver controller): o filho nasce na família de quem o criou. */
    @Transactional
    public UsuarioResponseDTO criarFilho(FilhoRequestDTO dto, UUID familiaId) {
        exigirEmailLivre(dto.email());
        return salvar(dto.nome(), dto.email(), dto.password(), Parentesco.FILHO, familiaId);
    }

    private Usuario buscarNaFamilia(UUID id, UUID familiaId) {
        return repository.findByIdAndFamiliaId(id, familiaId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    private void exigirEmailLivre(String email) {
        if (repository.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }
    }

    private UsuarioResponseDTO salvar(String nome, String email, String senha, Parentesco parentesco, UUID familiaId) {
        Usuario usuario = Usuario.builder()
                .nome(nome)
                .email(email)
                .password(passwordEncoder.encode(senha))
                .parentesco(parentesco)
                .familiaId(familiaId)
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

        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail(), usuario.getFamiliaId(), usuario.getParentesco());
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