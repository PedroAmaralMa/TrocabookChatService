package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;


import java.util.List;

public interface IChatService {

    MensagemDTO buscarPorId(String id);

    MensagemDTO enviarMensagem(MensagemDTO mensagemDTO);

    List<MensagemDTO> listarMensagensEntreUsuarios(String uidAnuncio, String uidRemetente, String uidDestinatario);

    List<MensagemDTO> listarMensagensPorUsuario(String uidRemetente);

    List<MensagemDTO> listarMensagensPorUsuarioDataEnvioDecrescente(String uidRemetente);

    MensagemDTO alterarMensagem(String id, String conteudo);

    boolean removerMensagem(String id);
}
