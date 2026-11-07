package com.br.ms_carteira.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.util.UUID;

@Component
public class JwtService {

    public record Sessao(UUID usuarioId, UUID familiaId, String parentesco) {
        public boolean ehPaiOuMae() {
            return "MAE".equals(parentesco) || "PAI".equals(parentesco);
        }
    }

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Sessao sessao(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token ausente");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token.substring(7))
                    .getPayload();

            return new Sessao(
                    UUID.fromString(claims.get("usuarioId", String.class)),
                    UUID.fromString(claims.get("familiaId", String.class)),
                    claims.get("parentesco", String.class));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido ou expirado");
        }
    }

    public Sessao exigirPaiOuMae(String token) {
        Sessao sessao = sessao(token);
        if (!sessao.ehPaiOuMae()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas pai ou mãe pode fazer isso");
        }
        return sessao;
    }
}
