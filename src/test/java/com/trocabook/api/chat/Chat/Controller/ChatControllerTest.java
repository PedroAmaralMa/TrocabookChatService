package com.trocabook.api.chat.Chat.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trocabook.api.chat.Chat.Config.RabbitMQConfig;
import com.trocabook.api.chat.Chat.Model.DTO.AtualizarMensagemDTO;
import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Service.IChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IChatService chatService;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("CT01 - Quando enviar mensagem válida (remetente < destinatário), deve despachar para RabbitMQ com status ACCEPTED")
    void quandoSalvarMensagemValidaRemetenteMenorQueDestinatario_entaoDespachaParaRabbitMQ() throws Exception {
        // DADO que as informações da mensagem são válidas e remetente precede destinatário
        MensagemDTO mensagemDTO = new MensagemDTO();
        mensagemDTO.setUidRemetente("userA");
        mensagemDTO.setUidDestinatario("userB");
        mensagemDTO.setUidAnuncio("anuncio123");
        mensagemDTO.setConteudo("Olá!");

        String expectedRoutingKey = "chat.message.anuncio123.userA.userB";

        // QUANDO o cliente faz uma requisição POST para salvar a mensagem
        mockMvc.perform(post("/api/chat/mensagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mensagemDTO)))

                // ENTÃO a mensagem é aceita e despachada para o RabbitMQ com status ACCEPTED
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Sucesso"))
                .andExpect(jsonPath("$.data.uidRemetente").value("userA"))
                .andExpect(jsonPath("$.data.conteudo").value("Olá!"));

        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.CHAT_EXCHANGE), eq(expectedRoutingKey), any(MensagemDTO.class));
    }

    @Test
    @DisplayName("CT02 - Quando enviar mensagem válida (remetente > destinatário), deve ordenar chaves no RabbitMQ")
    void quandoSalvarMensagemValidaRemetenteMaiorQueDestinatario_entaoOrdenaChavesNoRabbitMQ() throws Exception {
        // DADO que o identificador do remetente é lexicograficamente maior que o do destinatário
        MensagemDTO mensagemDTO = new MensagemDTO();
        mensagemDTO.setUidRemetente("userZ");
        mensagemDTO.setUidDestinatario("userA");
        mensagemDTO.setUidAnuncio("anuncio123");
        mensagemDTO.setConteudo("Olá!");

        String expectedRoutingKey = "chat.message.anuncio123.userA.userZ";

        // QUANDO o cliente faz a requisição POST para salvar a mensagem
        mockMvc.perform(post("/api/chat/mensagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mensagemDTO)))

                // ENTÃO a rota do RabbitMQ deve ser construída com u1 menor que u2 e retornar 202 ACCEPTED
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("success"));

        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.CHAT_EXCHANGE), eq(expectedRoutingKey), any(MensagemDTO.class));
    }

    @Test
    @DisplayName("CT03 - Quando ocorrer erro ao despachar mensagem no RabbitMQ, deve retornar status 500")
    void quandoSalvarMensagemComErroNoBroker_entaoRetornaInternalServerError() throws Exception {
        // DADO que ocorre uma falha inesperada ao tentar despachar a mensagem para o RabbitMQ
        MensagemDTO mensagemDTO = new MensagemDTO();
        mensagemDTO.setUidRemetente("user1");
        mensagemDTO.setUidDestinatario("user2");
        mensagemDTO.setUidAnuncio("anuncio123");

        doThrow(new RuntimeException("Falha de conexão com o broker RabbitMQ"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        try {
            // QUANDO o cliente realiza a requisição POST para salvar a mensagem
            MvcResult resultado = mockMvc.perform(post("/api/chat/mensagens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(mensagemDTO)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andReturn();

            // ENTÃO o código de status retornado deve ser exatamente 500
            assertEquals(500, resultado.getResponse().getStatus());
        } catch (Exception e) {
            fail("Exceção não esperada durante o teste de erro no broker: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("CT04 - Quando buscar mensagem por ID existente, deve retornar 200 OK com dados da mensagem")
    void quandoBuscarPorIdExistente_entaoRetornaOkComMensagem() throws Exception {
        // DADO que existe uma mensagem para o ID informado
        String id = "64a5f2e1bcf123456789abcd";
        MensagemDTO msg = new MensagemDTO();
        msg.setId(id);
        msg.setConteudo("Mensagem de teste");

        when(chatService.buscarPorId(id)).thenReturn(msg);

        // QUANDO o cliente faz uma requisição GET com o ID
        mockMvc.perform(get("/api/chat/mensagens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))

                // ENTÃO o status retornado é 200 OK e a mensagem é retornada no corpo
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Mensagem encontrada com sucesso"))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.conteudo").value("Mensagem de teste"));

        verify(chatService, times(1)).buscarPorId(id);
    }

    @Test
    @DisplayName("CT05 - Quando buscar mensagem por ID inexistente, deve retornar 404 NOT FOUND")
    void quandoBuscarPorIdInexistente_entaoRetornaNotFound() throws Exception {
        // DADO que a mensagem não foi encontrada para o ID informado
        String id = "idInexistente";
        when(chatService.buscarPorId(id)).thenReturn(null);

        // QUANDO o cliente consulta por ID inexistente
        mockMvc.perform(get("/api/chat/mensagens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))

                // ENTÃO deve retornar status 404 NOT FOUND
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Mensagem não encontrada"));

        verify(chatService, times(1)).buscarPorId(id);
    }

    @Test
    @DisplayName("CT06 - Quando ocorrer erro ao buscar mensagem por ID, deve retornar 500 INTERNAL SERVER ERROR")
    void quandoBuscarPorIdComErro_entaoRetornaInternalServerError() throws Exception {
        // DADO que o serviço lança uma exceção inesperada ao consultar por ID
        String id = "64a5f2e1bcf123456789abcd";
        when(chatService.buscarPorId(id)).thenThrow(new RuntimeException("Erro de leitura no banco"));

        try {
            // QUANDO o cliente realiza a busca por ID
            MvcResult resultado = mockMvc.perform(get("/api/chat/mensagens/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andReturn();

            // ENTÃO a asserção valida o status 500
            assertEquals(500, resultado.getResponse().getStatus());
        } catch (Exception e) {
            fail("Exceção inesperada no teste de buscarPorIdComErro: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("CT07 - Quando listar mensagens apenas com remetente, deve retornar conversas recentes")
    void quandoListarMensagensApenasComRemetente_entaoRetornaConversasRecentes() throws Exception {
        // DADO que são solicitadas conversas recentes do remetente (sem destinatário)
        MensagemDTO msg = new MensagemDTO();
        msg.setId("1");
        msg.setUidRemetente("user1");
        msg.setConteudo("Última mensagem");

        when(chatService.listarMensagensPorUsuarioDataEnvioDecrescente("user1"))
                .thenReturn(List.of(msg));

        // QUANDO a requisição GET é disparada sem o parâmetro destinatario
        mockMvc.perform(get("/api/chat/mensagens")
                        .param("remetente", "user1")
                        .contentType(MediaType.APPLICATION_JSON))

                // ENTÃO retorna 200 OK com as mensagens recentes
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Mensagens Carregadas"))
                .andExpect(jsonPath("$.data").isArray());

        verify(chatService, times(1)).listarMensagensPorUsuarioDataEnvioDecrescente("user1");
        verify(chatService, never()).listarMensagensEntreUsuarios(any(), any(), any());
    }

    @Test
    @DisplayName("CT08 - Quando listar mensagens informando remetente e destinatário, deve retornar histórico entre eles")
    void quandoListarMensagensComRemetenteEDestinatario_entaoRetornaHistoricoEntreUsuarios() throws Exception {
        // DADO que são informados remetente, destinatário e anúncio
        MensagemDTO msg = new MensagemDTO();
        msg.setId("1");
        msg.setUidRemetente("user1");
        msg.setUidDestinatario("user2");
        msg.setUidAnuncio("anuncio1");
        msg.setConteudo("Histórico de conversa");

        when(chatService.listarMensagensEntreUsuarios("anuncio1", "user1", "user2"))
                .thenReturn(List.of(msg));

        // QUANDO a requisição GET é disparada com os parâmetros informados
        mockMvc.perform(get("/api/chat/mensagens")
                        .param("remetente", "user1")
                        .param("destinatario", "user2")
                        .param("anuncio", "anuncio1")
                        .contentType(MediaType.APPLICATION_JSON))

                // ENTÃO retorna 200 OK com o histórico da conversa entre os dois usuários
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].conteudo").value("Histórico de conversa"));

        verify(chatService, times(1)).listarMensagensEntreUsuarios("anuncio1", "user1", "user2");
    }

    @Test
    @DisplayName("CT09 - Quando ocorrer erro ao listar mensagens, deve retornar 500 INTERNAL SERVER ERROR")
    void quandoListarMensagensComErro_entaoRetornaInternalServerError() throws Exception {
        // DADO que ocorre um erro ao listar mensagens no serviço
        when(chatService.listarMensagensPorUsuarioDataEnvioDecrescente("user1"))
                .thenThrow(new RuntimeException("Erro ao consultar mensagens"));

        try {
            // QUANDO a requisição GET é disparada
            MvcResult resultado = mockMvc.perform(get("/api/chat/mensagens")
                            .param("remetente", "user1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andReturn();

            // ENTÃO validação robusta de código 500
            assertEquals(500, resultado.getResponse().getStatus());
        } catch (Exception e) {
            fail("Exceção inesperada no teste de listarMensagensComErro: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("CT10 - Quando atualizar mensagem existente com sucesso, deve notificar WebSocket e retornar 200 OK")
    void quandoAtualizarMensagemExistente_entaoNotificaWebSocketERetornaOk() throws Exception {
        // DADO que existe uma mensagem e uma requisição de atualização válida
        String id = "msg123";
        AtualizarMensagemDTO dto = new AtualizarMensagemDTO();
        dto.setConteudo("Texto corrigido");

        MensagemDTO mensagemAlterada = new MensagemDTO();
        mensagemAlterada.setId(id);
        mensagemAlterada.setUidRemetente("userA");
        mensagemAlterada.setUidDestinatario("userB");
        mensagemAlterada.setUidAnuncio("anuncio1");
        mensagemAlterada.setConteudo("Texto corrigido");

        when(chatService.alterarMensagem(id, "Texto corrigido")).thenReturn(mensagemAlterada);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // QUANDO a requisição PUT é realizada
        mockMvc.perform(put("/api/chat/mensagens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))

                // ENTÃO a mensagem é atualizada, evento WebSocket enviado e retorno é 200 OK
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Mensagem alterada com sucesso"))
                .andExpect(jsonPath("$.data.conteudo").value("Texto corrigido"));

        verify(chatService, times(1)).alterarMensagem(id, "Texto corrigido");
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/conversa.anuncio1.userA.userB"), any(Object.class));
    }

    @Test
    @DisplayName("CT11 - Quando atualizar mensagem existente com remetente lexicograficamente maior, deve ordenar tópico WebSocket")
    void quandoAtualizarMensagemComRemetenteMaior_entaoOrdenaTopicoWebSocket() throws Exception {
        // DADO que o remetente é maior que o destinatário na mensagem atualizada
        String id = "msg123";
        AtualizarMensagemDTO dto = new AtualizarMensagemDTO();
        dto.setConteudo("Conteúdo atualizado");

        MensagemDTO mensagemAlterada = new MensagemDTO();
        mensagemAlterada.setId(id);
        mensagemAlterada.setUidRemetente("userZ");
        mensagemAlterada.setUidDestinatario("userA");
        mensagemAlterada.setUidAnuncio("anuncio1");
        mensagemAlterada.setConteudo("Conteúdo atualizado");

        when(chatService.alterarMensagem(id, "Conteúdo atualizado")).thenReturn(mensagemAlterada);

        // QUANDO a requisição PUT é disparada
        mockMvc.perform(put("/api/chat/mensagens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))

                // ENTÃO o tópico do WebSocket deve ser ordenado (/topic/conversa.anuncio1.userA.userZ)
                .andExpect(status().isOk());

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/conversa.anuncio1.userA.userZ"), any(Object.class));
    }

    @Test
    @DisplayName("CT12 - Quando atualizar mensagem inexistente, deve retornar 404 NOT FOUND")
    void quandoAtualizarMensagemInexistente_entaoRetornaNotFound() throws Exception {
        // DADO que o serviço retorna nulo indicando que a mensagem não existe
        String id = "msgInexistente";
        AtualizarMensagemDTO dto = new AtualizarMensagemDTO();
        dto.setConteudo("Conteúdo");

        when(chatService.alterarMensagem(id, "Conteúdo")).thenReturn(null);

        // QUANDO a requisição PUT é executada
        mockMvc.perform(put("/api/chat/mensagens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))

                // ENTÃO retorna status 404 NOT FOUND
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Mensagem não encontrada"));

        verify(chatService, times(1)).alterarMensagem(id, "Conteúdo");
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("CT13 - Quando ocorrer erro ao atualizar mensagem, deve retornar 500 INTERNAL SERVER ERROR")
    void quandoAtualizarMensagemComErro_entaoRetornaInternalServerError() throws Exception {
        // DADO que ocorre um erro inesperado ao atualizar a mensagem
        String id = "msg123";
        AtualizarMensagemDTO dto = new AtualizarMensagemDTO();
        dto.setConteudo("Conteúdo");

        when(chatService.alterarMensagem(id, "Conteúdo")).thenThrow(new RuntimeException("Falha no banco"));

        try {
            // QUANDO a requisição PUT é executada
            MvcResult resultado = mockMvc.perform(put("/api/chat/mensagens/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andReturn();

            // ENTÃO validação robusta com assertEquals
            assertEquals(500, resultado.getResponse().getStatus());
        } catch (Exception e) {
            fail("Exceção inesperada no teste de atualizarMensagemComErro: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("CT14 - Quando excluir mensagem com sucesso e mensagem em cache, deve notificar WebSocket e retornar 200 OK")
    void quandoExcluirMensagemComSucesso_entaoNotificaWebSocketERetornaOk() throws Exception {
        // DADO que a mensagem existe e é removida com sucesso
        String id = "msg123";
        MensagemDTO msgExistente = new MensagemDTO();
        msgExistente.setId(id);
        msgExistente.setUidRemetente("user1");
        msgExistente.setUidDestinatario("user2");
        msgExistente.setUidAnuncio("anuncio1");

        when(chatService.buscarPorId(id)).thenReturn(msgExistente);
        when(chatService.removerMensagem(id)).thenReturn(true);

        // QUANDO a requisição DELETE é executada
        mockMvc.perform(delete("/api/chat/mensagens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))

                // ENTÃO retorna status 200 OK e notifica a exclusão via WebSocket
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Mensagem removida com sucesso"));

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/conversa.anuncio1.user1.user2"), any(Object.class));
    }

    @Test
    @DisplayName("CT15 - Quando excluir mensagem com sucesso mas mensagemExistente nula, não notifica WebSocket e retorna 200 OK")
    void quandoExcluirMensagemComSucessoSemMensagemExistente_entaoRetornaOkSemNotificarWebSocket() throws Exception {
        // DADO que buscarPorId retorna null mas removerMensagem retorna true
        String id = "msg123";
        when(chatService.buscarPorId(id)).thenReturn(null);
        when(chatService.removerMensagem(id)).thenReturn(true);

        // QUANDO a requisição DELETE é executada
        mockMvc.perform(delete("/api/chat/mensagens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))

                // ENTÃO retorna 200 OK e não tenta notificar o WebSocket
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mensagem removida com sucesso"));

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("CT16 - Quando removerMensagem retornar false, deve retornar 404 NOT FOUND")
    void quandoExcluirMensagemInexistenteRetornandoFalse_entaoRetornaNotFound() throws Exception {
        // DADO que a mensagem não foi removida (removerMensagem retorna false)
        String id = "msg123";
        when(chatService.buscarPorId(id)).thenReturn(null);
        when(chatService.removerMensagem(id)).thenReturn(false);

        // QUANDO a requisição DELETE é executada
        mockMvc.perform(delete("/api/chat/mensagens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))

                // ENTÃO retorna 404 NOT FOUND
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Mensagem não encontrada"));
    }

    @Test
    @DisplayName("CT17 - Quando exclusão lançar IllegalStateException, deve retornar 400 BAD REQUEST")
    void quandoExcluirMensagemLancarIllegalStateException_entaoRetornaBadRequest() throws Exception {
        // DADO que o serviço lança IllegalStateException na exclusão
        String id = "msg123";
        when(chatService.buscarPorId(id)).thenReturn(null);
        when(chatService.removerMensagem(id)).thenThrow(new IllegalStateException("Mensagem não pode ser excluída após 24h"));

        try {
            // QUANDO a requisição DELETE é executada
            MvcResult resultado = mockMvc.perform(delete("/api/chat/mensagens/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Mensagem não pode ser excluída após 24h"))
                    .andReturn();

            // ENTÃO asserção robusta de status 400
            assertEquals(400, resultado.getResponse().getStatus());
        } catch (Exception e) {
            fail("Exceção inesperada no teste de IllegalStateException na exclusão: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("CT18 - Quando exclusão lançar Exception genérica, deve retornar 500 INTERNAL SERVER ERROR")
    void quandoExcluirMensagemLancarGenericException_entaoRetornaInternalServerError() throws Exception {
        // DADO que o serviço lança uma RuntimeException genérica na exclusão
        String id = "msg123";
        when(chatService.buscarPorId(id)).thenReturn(null);
        when(chatService.removerMensagem(id)).thenThrow(new RuntimeException("Falha catastrófica no banco"));

        try {
            // QUANDO a requisição DELETE é executada
            MvcResult resultado = mockMvc.perform(delete("/api/chat/mensagens/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andReturn();

            // ENTÃO asserção robusta de status 500
            assertEquals(500, resultado.getResponse().getStatus());
        } catch (Exception e) {
            fail("Exceção inesperada no teste de Exception genérica na exclusão: " + e.getMessage());
        }
    }
}
