package com.br.ms_usuario.controller;

import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.dto.LoginRequestDTO;
import com.br.ms_usuario.domain.dto.UsuarioRequestDTO;
import com.br.ms_usuario.domain.dto.UsuarioResponseDTO;
import com.br.ms_usuario.exceptions.CredenciaisInvalidasException;
import com.br.ms_usuario.exceptions.EmailJaCadastradoException;
import com.br.ms_usuario.exceptions.GlobalExceptionHandler;
import com.br.ms_usuario.exceptions.UsuarioNaoEncontradoException;
import com.br.ms_usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService service;

    @InjectMocks
    private UsuarioController controller;

    private MockMvc mockMvc;

    private static final String CADASTRO = """
            {"nome": "Ana", "email": "ana@casa.com", "password": "123456", "parentesco": "MAE"}
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registrar_deveRetornar201() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.registrar(any(UsuarioRequestDTO.class)))
                .thenReturn(new UsuarioResponseDTO(id, "Ana", "ana@casa.com", Parentesco.MAE));

        mockMvc.perform(post("/usuarios/registrar").contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registrar_deveRetornar400ComDetalhesQuandoCamposInvalidos() throws Exception {
        mockMvc.perform(post("/usuarios/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "", "email": "nao-e-email", "password": "123", "parentesco": null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.length()").value(4));

        verifyNoInteractions(service);
    }

    @Test
    void registrar_deveRetornar409QuandoEmailJaCadastrado() throws Exception {
        when(service.registrar(any(UsuarioRequestDTO.class))).thenThrow(new EmailJaCadastradoException("ana@casa.com"));

        mockMvc.perform(post("/usuarios/registrar").contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isConflict());
    }

    @Test
    void login_deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
        when(service.login(any(LoginRequestDTO.class))).thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@casa.com", "password": "errada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou senha inválidos"));
    }

    @Test
    void buscarPorId_deveRetornar404QuandoUsuarioNaoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getUsuarioPorId(id)).thenThrow(new UsuarioNaoEncontradoException(id));

        mockMvc.perform(get("/usuarios/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPorParentesco_deveRetornar400ParaParentescoInexistente() throws Exception {
        mockMvc.perform(get("/usuarios/parentesco/TIO"))
                .andExpect(status().isBadRequest());
    }
}
