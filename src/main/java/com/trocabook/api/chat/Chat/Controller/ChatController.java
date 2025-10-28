package com.trocabook.api.chat.Chat.Controller;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;
import com.trocabook.api.chat.Chat.Response.ApiResponse;
import com.trocabook.api.chat.Chat.Service.IChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/chat")
public class ChatController {

    final IChatService chatService;

    @Autowired
    public ChatController(IChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/mensagens")
    public ResponseEntity<ApiResponse<MensagemDTO>> salvarMensagem(@RequestBody MensagemDTO mensagem){
        try{
            MensagemDTO mensagemSalva = chatService.enviarMensagem(mensagem);
            ApiResponse<MensagemDTO> response = new ApiResponse<>(mensagemSalva, "Mensagem Salva com sucesso");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch(IllegalStateException e){
            ApiResponse<MensagemDTO> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e){
            ApiResponse<MensagemDTO> response = new ApiResponse<>("Erro ao enviar Mensagem");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/mensagens")
    public ResponseEntity<ApiResponse<List<MensagemDTO>>> listarMensagens(
            @RequestParam(name = "remetente") int cdRemetente,
            @RequestParam(name = "destinatario", required = false) Integer cdDestinatario,
            @RequestParam(name = "usuarioLivro", required = false) Integer cdUsuarioLivro) {

        try {
            List<MensagemDTO> mensagens;

            if (cdDestinatario == null) {
                mensagens = chatService.listarMensagensPorUsuarioDataEnvioDecrescente(cdRemetente);
            } else {
                mensagens = chatService.listarMensagensEntreUsuarios(cdUsuarioLivro, cdRemetente, cdDestinatario);
            }

            ApiResponse<List<MensagemDTO>> response = new ApiResponse<>(mensagens, "Mensagens Carregadas");
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception e) {
            ApiResponse<List<MensagemDTO>> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/mensagens/{id}")
    public ResponseEntity<ApiResponse<MensagemDTO>> atualizarMensagem(@PathVariable String id, @RequestBody String conteudo){
        try {
            Mensagem mensagem = chatService.alterarMensagem(id, conteudo);
            if (mensagem != null){
                MensagemDTO mensagemDTO = new MensagemDTO(mensagem);
                ApiResponse<MensagemDTO> response = new ApiResponse<>(mensagemDTO, "Mensagem alterada com sucesso");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
            ApiResponse<MensagemDTO> response = new ApiResponse<>("Mensagem não encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalStateException e){
            ApiResponse<MensagemDTO> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e){
            ApiResponse<MensagemDTO> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/mensagens/{id}")
    public ResponseEntity<ApiResponse<Void>> excluirMensagem(@PathVariable String id){
        try {
            boolean resultado = chatService.removerMensagem(id);
            if (resultado){
                ApiResponse<Void> response = new ApiResponse<>(null, "Mensagem removida com sucesso");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
            ApiResponse<Void> response = new ApiResponse<>("Mensagem não encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalStateException e){
            ApiResponse<Void> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e){
            ApiResponse<Void> response = new ApiResponse<>(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }





}
