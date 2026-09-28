package com.br.ms_tarefa.controller;

import com.br.ms_tarefa.domain.Status;
import com.br.ms_tarefa.domain.dto.TarefaRequestDTO;
import com.br.ms_tarefa.domain.dto.TarefaResponseDTO;
import com.br.ms_tarefa.service.TarefaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TarefaControllerTest {

    private static final String AUTH = "Bearer token";

    @Mock
    private TarefaService service;

    @InjectMocks
    private TarefaController controller;

    private MockMvc mockMvc;

    private final UUID tarefaId = UUID.randomUUID();
    private final UUID filhoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private TarefaResponseDTO resposta(Status status) {
        return new TarefaResponseDTO(tarefaId, "Lavar a louça", null, filhoId, UUID.randomUUID(), status);
    }

    @Test
    void criar_deveRetornar201() throws Exception {
        when(service.criar(any(TarefaRequestDTO.class), eq(AUTH))).thenReturn(resposta(Status.PENDENTE));

        mockMvc.perform(post("/tarefas")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Lavar a louça", "usuarioId": "%s"}
                                """.formatted(filhoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void criar_deveRetornar400SemTituloOuResponsavel() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void criar_deveRetornar400SemHeaderAuthorization() throws Exception {
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Lavar a louça", "usuarioId": "%s"}
                                """.formatted(filhoId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concluir_deveRetornarTarefaAguardandoAprovacao() throws Exception {
        when(service.concluir(tarefaId, AUTH)).thenReturn(resposta(Status.AGUARDANDO_APROVACAO));

        mockMvc.perform(patch("/tarefas/{id}/concluir", tarefaId).header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"));
    }

    @Test
    void aprovar_deveRepassarStatusDoServiceQuandoNaoEPaiOuMae() throws Exception {
        when(service.aprovar(tarefaId, AUTH))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas pai ou mãe pode fazer isso"));

        mockMvc.perform(patch("/tarefas/{id}/aprovar", tarefaId).header("Authorization", AUTH))
                .andExpect(status().isForbidden());
    }
}
