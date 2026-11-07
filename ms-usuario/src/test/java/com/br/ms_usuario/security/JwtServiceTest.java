package com.br.ms_usuario.security;

import com.br.ms_usuario.domain.Parentesco;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "teste-secret-usado-somente-em-testes-nao-usar-em-producao");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    private String token() {
        return jwtService.generateToken(usuarioId, "ana@casa.com", UUID.randomUUID(), Parentesco.MAE);
    }

    @Test
    void generateToken_deveCarregarEmailEIdDoUsuario() {
        String token = token();

        assertThat(jwtService.extractEmail(token)).isEqualTo("ana@casa.com");
        assertThat(jwtService.extractUsuarioId(token)).isEqualTo(usuarioId);
    }

    @Test
    void isTokenValido_deveSerFalsoParaOutroEmail() {
        String token = token();

        assertThat(jwtService.isTokenValido(token, "ana@casa.com")).isTrue();
        assertThat(jwtService.isTokenValido(token, "outro@casa.com")).isFalse();
    }

    @Test
    void tokenExpirado_deveSerRejeitado() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1_000L);
        String token = token();

        assertThatThrownBy(() -> jwtService.extractEmail(token)).isInstanceOf(ExpiredJwtException.class);
    }
}
