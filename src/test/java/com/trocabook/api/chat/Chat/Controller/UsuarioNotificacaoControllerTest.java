package com.trocabook.api.chat.Chat.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trocabook.api.chat.Chat.Model.DTO.UsuarioStatusNotificacaoDTO;
import com.trocabook.api.chat.Chat.Service.NotificacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioNotificacaoController.class)
class UsuarioNotificacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificacaoService notificacaoService;

    @Test
    @DisplayName("CT01 - Quando registrar status com dados válidos, deve retornar status 200 OK")
    void quandoRegistrarStatusValido_entaoRetornaOk() throws Exception {
        // DADO que o DTO contém informações válidas de notificação do usuário
        UsuarioStatusNotificacaoDTO dto = new UsuarioStatusNotificacaoDTO();
        dto.setUidUsuario("user123");
        dto.setEmail("user@trocabook.com");
        dto.setFcmToken("fcm-token-valido-12345");

        doNothing().when(notificacaoService).salvarStatusUsuario(any(UsuarioStatusNotificacaoDTO.class));

        // QUANDO a requisição POST é enviada para /api/chat/notificacoes/status-usuario
        mockMvc.perform(post("/api/chat/notificacoes/status-usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))

                // ENTÃO deve retornar status 200 OK e mensagem de sucesso
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Status de notificação atualizado com sucesso"));

        verify(notificacaoService, times(1)).salvarStatusUsuario(any(UsuarioStatusNotificacaoDTO.class));
    }

    @Test
    @DisplayName("CT02 - Quando registrar status com uidUsuario nulo, deve retornar status 400 BAD REQUEST")
    void quandoRegistrarStatusComUidUsuarioNulo_entaoRetornaBadRequest() throws Exception {
        // DADO que o DTO possui uidUsuario nulo
        UsuarioStatusNotificacaoDTO dto = new UsuarioStatusNotificacaoDTO();
        dto.setUidUsuario(null);
        dto.setEmail("user@trocabook.com");

        // QUANDO a requisição POST é enviada
        mockMvc.perform(post("/api/chat/notificacoes/status-usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))

                // ENTÃO deve retornar status 400 BAD REQUEST com mensagem informativa
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("uidUsuario é obrigatório"));

        verify(notificacaoService, never()).salvarStatusUsuario(any());
    }

    @Test
    @DisplayName("CT03 - Quando registrar status com uidUsuario vazio/em branco, deve retornar status 400 BAD REQUEST")
    void quandoRegistrarStatusComUidUsuarioEmBranco_entaoRetornaBadRequest() throws Exception {
        // DADO que o DTO possui uidUsuario apenas com espaços em branco
        UsuarioStatusNotificacaoDTO dto = new UsuarioStatusNotificacaoDTO();
        dto.setUidUsuario("   ");
        dto.setEmail("user@trocabook.com");

        // QUANDO a requisição POST é enviada
        mockMvc.perform(post("/api/chat/notificacoes/status-usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))

                // ENTÃO deve retornar status 400 BAD REQUEST
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("uidUsuario é obrigatório"));

        verify(notificacaoService, never()).salvarStatusUsuario(any());
    }

    @Test
    @DisplayName("CT04 - Quando ocorrer exceção ao salvar status, deve retornar status 500 INTERNAL SERVER ERROR")
    void quandoRegistrarStatusLancarExcecao_entaoRetornaInternalServerError() throws Exception {
        // DADO que o serviço lança uma exceção durante o salvamento
        UsuarioStatusNotificacaoDTO dto = new UsuarioStatusNotificacaoDTO();
        dto.setUidUsuario("user123");
        dto.setEmail("user@trocabook.com");

        doThrow(new RuntimeException("Falha no banco de dados ao salvar status"))
                .when(notificacaoService).salvarStatusUsuario(any(UsuarioStatusNotificacaoDTO.class));

        try {
            // QUANDO a requisição POST é enviada
            MvcResult resultado = mockMvc.perform(post("/api/chat/notificacoes/status-usuario")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andReturn();

            // ENTÃO asserção robusta de código 500
            assertEquals(500, resultado.getResponse().getStatus());
        } catch (Exception e) {
            fail("Exceção inesperada no teste de registrarStatus com erro: " + e.getMessage());
        }

        verify(notificacaoService, times(1)).salvarStatusUsuario(any(UsuarioStatusNotificacaoDTO.class));
    }
}

