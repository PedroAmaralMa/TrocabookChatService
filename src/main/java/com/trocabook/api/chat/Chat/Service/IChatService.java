package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;

import java.util.List;

public interface IChatService {
    Mensagem enviarMensagem(MensagemDTO mensagemDTO);

    List<Mensagem> listarMensagensEntreUsuarios(int cdUsuarioDestinatario, int cdUsuarioRemetente);

    List<Mensagem> listarMensagensPorUsuario(int cdUsuario);

    List<Mensagem> listarMensagensPorUsuarioDataEnvioDecrescente(int cdUsuarioRemtente);
}
