package com.br.ms_tarefa.client;

import com.br.ms_tarefa.domain.Parentesco;
import com.br.ms_tarefa.domain.dto.UsuarioResponseDTO;
import com.br.ms_tarefa.exceptions.UsuarioNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UsuarioClientTest {

    private MockRestServiceServer server;
    private UsuarioClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UsuarioClient(builder, "http://ms-usuario:8080");
    }

    @Test
    void buscarUsuario_deveRepassarOTokenEConverterResposta() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://ms-usuario:8080/usuarios/" + id))
                .andExpect(header("Authorization", "Bearer abc"))
                .andRespond(withSuccess("""
                        {"id": "%s", "nome": "Pedro", "email": "pedro@casa.com", "parentesco": "FILHO"}
                        """.formatted(id), MediaType.APPLICATION_JSON));

        UsuarioResponseDTO usuario = client.buscarUsuario(id, "Bearer abc");

        assertThat(usuario.parentesco()).isEqualTo(Parentesco.FILHO);
        server.verify();
    }

    @Test
    void buscarUsuario_deveLancarUsuarioNaoEncontradoQuandoMsUsuarioResponde404() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://ms-usuario:8080/usuarios/" + id))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.buscarUsuario(id, "Bearer abc"))
                .isInstanceOf(UsuarioNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }
}
