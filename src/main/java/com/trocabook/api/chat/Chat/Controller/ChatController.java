package com.trocabook.api.chat.Chat.Controller;

import com.trocabook.api.chat.Chat.Config.RabbitMQConfig;
import com.trocabook.api.chat.Chat.Model.DTO.AtualizarMensagemDTO;
import com.trocabook.api.chat.Chat.Model.DTO.EventoChatDTO;
import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Response.ApiResponse;
import com.trocabook.api.chat.Chat.Service.IChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final IChatService chatService;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatController(
            IChatService chatService,
            RabbitTemplate rabbitTemplate,
            SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.rabbitTemplate = rabbitTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/mensagens")
    public ResponseEntity<ApiResponse<MensagemDTO>> salvarMensagem(@RequestBody MensagemDTO mensagem) {
        log.info("Requisição POST /api/chat/mensagens recebida. Remetente: '{}', Destinatário: '{}', Anúncio: '{}'",
                mensagem.getUidRemetente(), mensagem.getUidDestinatario(), mensagem.getUidAnuncio());
        try {
            String u1 = mensagem.getUidRemetente().compareTo(mensagem.getUidDestinatario()) < 0
                    ? mensagem.getUidRemetente() : mensagem.getUidDestinatario();
            String u2 = mensagem.getUidRemetente().compareTo(mensagem.getUidDestinatario()) < 0
                    ? mensagem.getUidDestinatario() : mensagem.getUidRemetente();

            String routingKey = "chat.message." + mensagem.getUidAnuncio() + "." + u1 + "." + u2;

            log.info("Enviando mensagem para a fila RabbitMQ. Exchange: '{}', RoutingKey: '{}'", RabbitMQConfig.CHAT_EXCHANGE, routingKey);
            rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, routingKey, mensagem);
            log.info("Mensagem despachada com sucesso para o RabbitMQ. RoutingKey: '{}'", routingKey);

            ApiResponse<MensagemDTO> response = new ApiResponse<>(mensagem, "Sucesso");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem para processamento no RabbitMQ: {}", e.getMessage(), e);
            ApiResponse<MensagemDTO> response = new ApiResponse<>("Erro ao enviar mensagem: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/mensagens/{id}")
    public ResponseEntity<ApiResponse<MensagemDTO>> buscarPorId(@PathVariable String id) {
        log.info("Requisição GET /api/chat/mensagens/{} recebida.", id);
        try {
            MensagemDTO mensagem = chatService.buscarPorId(id);

            if (mensagem != null) {
                log.info("Mensagem ID '{}' retornada com sucesso.", id);
                ApiResponse<MensagemDTO> response = new ApiResponse<>(mensagem, "Mensagem encontrada com sucesso");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }

            log.warn("Mensagem ID '{}' não encontrada na busca por ID.", id);
            ApiResponse<MensagemDTO> response = new ApiResponse<>("Mensagem não encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar mensagem por ID '{}': {}", id, e.getMessage(), e);
            ApiResponse<MensagemDTO> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/mensagens")
    public ResponseEntity<ApiResponse<List<MensagemDTO>>> listarMensagens(
            @RequestParam(name = "remetente") String uidRemetente,
            @RequestParam(name = "destinatario", required = false) String uidDestinatario,
            @RequestParam(name = "anuncio", required = false) String uidAnuncio) {

        log.info("Requisição GET /api/chat/mensagens recebida. Remetente: '{}', Destinatário: '{}', Anúncio: '{}'",
                uidRemetente, uidDestinatario, uidAnuncio);
        try {
            List<MensagemDTO> mensagens;

            if (uidDestinatario == null) {
                log.info("Listando conversas recentes para o remetente '{}'", uidRemetente);
                mensagens = chatService.listarMensagensPorUsuarioDataEnvioDecrescente(uidRemetente);
            } else {
                log.info("Listando histórico entre remetente '{}' e destinatário '{}' no anúncio '{}'",
                        uidRemetente, uidDestinatario, uidAnuncio);
                mensagens = chatService.listarMensagensEntreUsuarios(uidAnuncio, uidRemetente, uidDestinatario);
            }

            log.info("Total de {} mensagens retornadas na consulta.", mensagens.size());
            ApiResponse<List<MensagemDTO>> response = new ApiResponse<>(mensagens, "Mensagens Carregadas");
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception e) {
            log.error("Erro ao listar mensagens para o remetente '{}': {}", uidRemetente, e.getMessage(), e);
            ApiResponse<List<MensagemDTO>> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/mensagens/{id}")
    public ResponseEntity<ApiResponse<MensagemDTO>> atualizarMensagem(
            @PathVariable String id,
            @RequestBody AtualizarMensagemDTO dto) {

        log.info("Requisição PUT /api/chat/mensagens/{} recebida para atualização de conteúdo.", id);
        try {
            MensagemDTO mensagemAlterada = chatService.alterarMensagem(id, dto.getConteudo());

            if (mensagemAlterada != null) {
                log.info("Mensagem ID '{}' atualizada com sucesso. Propagando evento via WebSocket.", id);
                // Notifica os clientes do WebSocket que a mensagem foi editada em tempo real
                notificarEventoWebSocket(mensagemAlterada, EventoChatDTO.TipoEvento.MENSAGEM_EDITADA, null);

                ApiResponse<MensagemDTO> response = new ApiResponse<>(mensagemAlterada, "Mensagem alterada com sucesso");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }

            log.warn("Mensagem ID '{}' não encontrada para atualização.", id);
            ApiResponse<MensagemDTO> response = new ApiResponse<>("Mensagem não encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erro ao atualizar mensagem ID '{}': {}", id, e.getMessage(), e);
            ApiResponse<MensagemDTO> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/mensagens/{id}")
    public ResponseEntity<ApiResponse<Void>> excluirMensagem(@PathVariable String id) {
        log.info("Requisição DELETE /api/chat/mensagens/{} recebida para exclusão.", id);
        try {
            MensagemDTO mensagemExistente = chatService.buscarPorId(id);

            boolean resultado = chatService.removerMensagem(id);
            if (resultado) {
                if (mensagemExistente != null) {
                    log.info("Mensagem ID '{}' removida. Propagando evento de exclusão via WebSocket.", id);
                    notificarEventoWebSocket(mensagemExistente, EventoChatDTO.TipoEvento.MENSAGEM_EXCLUIDA, id);
                } else {
                    log.warn("Mensagem ID '{}' removida, porém dados prévios não estavam em cache para notificar WebSocket.", id);
                }

                ApiResponse<Void> response = new ApiResponse<>(null, "Mensagem removida com sucesso");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
            log.warn("Mensagem ID '{}' não encontrada para remoção.", id);
            ApiResponse<Void> response = new ApiResponse<>("Mensagem não encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalStateException e) {
            log.warn("Falha de regra de negócio ao excluir mensagem ID '{}': {}", id, e.getMessage());
            ApiResponse<Void> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir mensagem ID '{}': {}", id, e.getMessage(), e);
            ApiResponse<Void> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private void notificarEventoWebSocket(MensagemDTO mensagem, EventoChatDTO.TipoEvento tipo, String idExcluido) {
        // 1. Mantém a ordenação alfabética segura de u1 e u2
        String u1 = mensagem.getUidRemetente().compareTo(mensagem.getUidDestinatario()) < 0
                ? mensagem.getUidRemetente() : mensagem.getUidDestinatario();
        String u2 = mensagem.getUidRemetente().compareTo(mensagem.getUidDestinatario()) < 0
                ? mensagem.getUidDestinatario() : mensagem.getUidRemetente();

        // 2. Monta o evento que vai transitar no RabbitMQ
        EventoChatDTO evento = (tipo == EventoChatDTO.TipoEvento.MENSAGEM_EXCLUIDA)
                ? new EventoChatDTO(tipo, idExcluido)
                : new EventoChatDTO(tipo, mensagem);

        if (tipo == EventoChatDTO.TipoEvento.MENSAGEM_EXCLUIDA) {
            evento.setMensagem(mensagem); // Garante que o listener sabe qual é a conversa/anúncio
        }

        // 3. Monta uma routing key dinâmica baseada no padrão de entrega (casando com "chat.delivery.#")
        // Exemplo: chat.delivery.edit.<anuncio>.<u1>.<u2> ou chat.delivery.delete.<anuncio>.<u1>.<u2>
        String acao = (tipo == EventoChatDTO.TipoEvento.MENSAGEM_EXCLUIDA) ? "delete" : "edit";
        String routingKey = "chat.delivery." + acao + "." + mensagem.getUidAnuncio() + "." + u1 + "." + u2;

        log.info("Publicando evento WebSocket '{}' no RabbitMQ com routingKey '{}'", tipo, routingKey);
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, routingKey, evento);
    }
}
