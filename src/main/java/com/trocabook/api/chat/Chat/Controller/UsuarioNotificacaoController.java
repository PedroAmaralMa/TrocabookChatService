package com.trocabook.api.chat.Chat.Controller;

import com.trocabook.api.chat.Chat.Model.DTO.UsuarioStatusNotificacaoDTO;
import com.trocabook.api.chat.Chat.Response.ApiResponse;
import com.trocabook.api.chat.Chat.Service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/chat/notificacoes")
public class UsuarioNotificacaoController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioNotificacaoController.class);

    private final NotificacaoService notificacaoService;

    @Autowired
    public UsuarioNotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @PostMapping("/status-usuario")
    public ResponseEntity<ApiResponse<Void>> registrarOuAtualizarStatus(@RequestBody UsuarioStatusNotificacaoDTO dto) {
        log.info("Requisição POST /api/chat/notificacoes/status-usuario recebida para o usuário '{}'", dto.getUidUsuario());

        try {
            if (dto.getUidUsuario() == null || dto.getUidUsuario().isBlank()) {
                log.warn("Requisição de status-usuario rejeitada: 'uidUsuario' é obrigatório.");
                return ResponseEntity.badRequest().body(new ApiResponse<>("uidUsuario é obrigatório"));
            }

            notificacaoService.salvarStatusUsuario(dto);
            log.info("Status de notificação do usuário '{}' atualizado com sucesso via controller.", dto.getUidUsuario());

            return ResponseEntity.ok(new ApiResponse<>(null, "Status de notificação atualizado com sucesso"));
        } catch (Exception e) {
            log.error("Erro ao registrar/atualizar status de notificação do usuário '{}': {}", dto.getUidUsuario(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Erro ao atualizar status: " + e.getMessage()));
        }
    }
}
