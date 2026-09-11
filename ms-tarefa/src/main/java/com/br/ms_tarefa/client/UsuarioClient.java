package com.br.ms_tarefa.client;

import com.br.ms_tarefa.domain.dto.UsuarioResponseDTO;
import com.br.ms_tarefa.exceptions.UsuarioNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class UsuarioClient {

    private final RestClient restClient;

    public UsuarioClient(RestClient.Builder builder,
                         @Value("${ms-usuarios.url}") String usuarioServiceUrl) {
        this.restClient = builder.baseUrl(usuarioServiceUrl).build();
    }



    public UsuarioResponseDTO buscarUsuario(UUID usuarioId, String bearerToken) {
        return restClient.get()
                .uri("/usuarios/{id}", usuarioId)
                .header("Authorization", bearerToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError , (request, response) -> {
                    throw new UsuarioNaoEncontradoException(usuarioId);
                })
                .body(UsuarioResponseDTO.class);
    }
}
