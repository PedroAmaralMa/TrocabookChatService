package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;


import java.util.List;

public interface IChatService {
    MensagemDTO enviarMensagem(MensagemDTO mensagemDTO);

    List<MensagemDTO> listarMensagensEntreUsuarios(int cdUsuarioLivro, int cdUsuarioDestinatario, int cdUsuarioRemetente);

    List<MensagemDTO> listarMensagensPorUsuario(int cdUsuario);

    List<MensagemDTO> listarMensagensPorUsuarioDataEnvioDecrescente(int cdUsuarioRemtente);

    Mensagem alterarMensagem(String id, String conteudo);

    boolean removerMensagem(String id);
}
