package com.br.ms_tarefa.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "teste-secret-usado-somente-em-testes-nao-usar-em-producao";

    private final JwtService jwtService = new JwtService(SECRET);

    /** Gera o token do mesmo jeito que o ms-usuario, para garantir o contrato entre os serviços. */
    private String tokenEmitidoPeloMsUsuario(String secret, UUID usuarioId, long validadeMs) {
        return Jwts.builder()
                .subject("ana@casa.com")
                .claim("usuarioId", usuarioId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + validadeMs))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    @Test
    void autenticado_deveExtrairUsuarioIdDoTokenDoMsUsuario() {
        UUID id = UUID.randomUUID();

        assertThat(jwtService.autenticado("Bearer " + tokenEmitidoPeloMsUsuario(SECRET, id, 60_000))).isEqualTo(id);
    }

    @Test
    void familiaDe_deveExtrairAFamiliaDoToken() {
        UUID familia = UUID.randomUUID();
        String token = Jwts.builder()
                .subject("ana@casa.com")
                .claim("usuarioId", UUID.randomUUID().toString())
                .claim("familiaId", familia.toString())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();

        assertThat(jwtService.familiaDe("Bearer " + token)).isEqualTo(familia);
    }

    @Test
    void familiaDe_tokenAntigoSemFamiliaRecebe401() {
        String token = tokenEmitidoPeloMsUsuario(SECRET, UUID.randomUUID(), 60_000);

        assertThatThrownBy(() -> jwtService.familiaDe("Bearer " + token))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void autenticado_deveRetornar401SemHeaderBearer() {
        assertUnauthorized(null);
        assertUnauthorized("Basic YWJjOjEyMw==");
    }

    @Test
    void autenticado_deveRetornar401ParaTokenAssinadoComOutroSegredo() {
        String token = tokenEmitidoPeloMsUsuario("outro-segredo-com-tamanho-suficiente-para-hs256!!", UUID.randomUUID(), 60_000);

        assertUnauthorized("Bearer " + token);
    }

    @Test
    void autenticado_deveRetornar401ParaTokenExpirado() {
        assertUnauthorized("Bearer " + tokenEmitidoPeloMsUsuario(SECRET, UUID.randomUUID(), -1_000));
    }

    private void assertUnauthorized(String header) {
        assertThatThrownBy(() -> jwtService.autenticado(header))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
