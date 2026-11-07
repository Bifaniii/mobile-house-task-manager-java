package com.br.ms_tarefa.security;

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

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret){
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }


    public UUID autenticado(String token) {
        return claimUuid(token, "usuarioId");
    }

    /** Tokens emitidos antes das famílias não têm essa informação: respondem 401 e a pessoa entra de novo. */
    public UUID familiaDe(String token) {
        return claimUuid(token, "familiaId");
    }

    private UUID claimUuid(String token, String claim) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token ausente");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token.substring(7))
                    .getPayload();

            return UUID.fromString(claims.get(claim, String.class));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido ou expirado");
        }
    }

}
