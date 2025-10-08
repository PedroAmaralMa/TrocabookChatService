package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;
import com.trocabook.api.chat.Chat.Repository.MensagemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService implements IChatService{

    final MensagemRepository mensagemRepository;
    @Autowired
    public ChatService(MensagemRepository mensagemRepository){
        this.mensagemRepository = mensagemRepository;

    }

    public Mensagem enviarMensagem(MensagemDTO mensagemDTO){
        Mensagem mensagem = new Mensagem();
        mensagem.setCdUsuarioDestinatario(mensagemDTO.getCdUsuarioDestinatario());
        mensagem.setConteudo(mensagemDTO.getConteudo());
        mensagem.setCdUsuarioRemetente(mensagemDTO.getCdUsuarioRemetente());
        mensagem.setDataEnvio(LocalDateTime.now());
        return mensagemRepository.save(mensagem);



    }

    public List<Mensagem> listarMensagensEntreUsuarios(int cdUsuarioDestinatario, int cdUsuarioRemetente){
        return mensagemRepository.findByCdUsuarioRemetenteAndCdUsuarioDestinatarioOrCdUsuarioDestinatarioAndCdUsuarioRemetente(cdUsuarioRemetente, cdUsuarioDestinatario, cdUsuarioDestinatario, cdUsuarioRemetente);

    }

    public List<Mensagem> listarMensagensPorUsuario(int cdUsuarioRemetente){
        return mensagemRepository.findByCdUsuarioRemetente(cdUsuarioRemetente);

    }

    public List<Mensagem> listarMensagensPorUsuarioDataEnvioDecrescente(int cdUsuarioRemetente){
        return mensagemRepository.findByCdUsuarioRemetenteOrderByDataEnvioDesc(cdUsuarioRemetente);

    }


}
