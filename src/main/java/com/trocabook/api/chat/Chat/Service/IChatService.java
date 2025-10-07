package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;

import java.util.List;

public interface IChatService {
    MensagemResponse<T> enviarMensagem(MensagemDTO mensagemDTO);

    MensagemResponse<T> listarMensagensEntreUsuarios(int cdUsuarioDestinatario, int cdUsuarioRemetente);

    MensagemResponse<T> listarMensagensPorUsuario(int cdUsuario);
}
