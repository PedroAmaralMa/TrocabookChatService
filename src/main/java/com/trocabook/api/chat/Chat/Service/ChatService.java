package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;
import com.trocabook.api.chat.Chat.Repository.MensagemRepository;
import com.trocabook.api.chat.Chat.Response.MensagemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService implements IChatService{

    final MensagemRepository mensagemRepository;
    @Autowired
    ChatService(MensagemRepository mensagemRepository){
        this.mensagemRepository = mensagemRepository;

    }

    public MensagemResponse<T> enviarMensagem(MensagemDTO mensagemDTO){
        Mensagem mensagem = new Mensagem();
        mensagem.setCdUsuarioDestinatario(mensagemDTO.getCdUsuarioDestinatario());
        mensagem.setConteudo(mensagemDTO.getConteudo());
        mensagem.setCdUsuarioRemetente(mensagemDTO.getCdUsuarioRemetente());
        mensagem.setDataEnvio(LocalDateTime.now());
        mensagemRepository.save(mensagem);
        return new MensagemResponse<T>(true, mensagem, "Mensagem salva");


    }

    public MensagemResponse<T> listarMensagensEntreUsuarios(int cdUsuarioDestinatario, int cdUsuarioRemetente){
        List<Mensagem> mensagens = mensagemRepository.findByCdUsuarioRemetenteAndCdUsuarioDestinatarioOrCdUsuarioDestinatarioAndCdUsuarioRemetente(cdUsuarioRemetente, cdUsuarioDestinatario, cdUsuarioDestinatario, cdUsuarioRemetente);
        return new MensagemResponse<T>(true, mensagens, "Mensagens carregadas");
    }

    public MensagemResponse<T> listarMensagensPorUsuario(int cdUsuarioRemetente){
        List<Mensagem> mensagens = mensagemRepository.findByCdUsuarioRemetente(cdUsuarioRemetente);
        return new MensagemResponse<T>(true, mensagens, "Mensagens do usuario carregadas");
    }


}
