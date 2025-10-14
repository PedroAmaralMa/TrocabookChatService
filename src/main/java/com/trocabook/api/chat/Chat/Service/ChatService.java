package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;
import com.trocabook.api.chat.Chat.Repository.MensagemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService implements IChatService{

    final MensagemRepository mensagemRepository;
    @Autowired
    public ChatService(MensagemRepository mensagemRepository){
        this.mensagemRepository = mensagemRepository;

    }

    public MensagemDTO enviarMensagem(MensagemDTO mensagemDTO){
        Mensagem mensagem = new Mensagem();
        mensagem.setCdUsuarioDestinatario(mensagemDTO.getCdUsuarioDestinatario());
        mensagem.setConteudo(mensagemDTO.getConteudo());
        mensagem.setCdUsuarioRemetente(mensagemDTO.getCdUsuarioRemetente());
        mensagem.setCdUsuarioLivro(mensagemDTO.getCdUsuarioLivro());
        mensagem.setDataEnvio(LocalDateTime.now());
        mensagemRepository.save(mensagem);
        return mensagemDTO;



    }

    public List<MensagemDTO> listarMensagensEntreUsuarios(int cdUsuarioDestinatario, int cdUsuarioRemetente){
        return transformarListaMensagemEmListaMensagemDTO(
                mensagemRepository.findByCdUsuarioRemetenteAndCdUsuarioDestinatarioOrCdUsuarioDestinatarioAndCdUsuarioRemetenteOrderByDataEnvioAsc
                (cdUsuarioRemetente, cdUsuarioDestinatario, cdUsuarioDestinatario, cdUsuarioRemetente));

    }

    public List<MensagemDTO> listarMensagensPorUsuario(int cdUsuarioRemetente){
        return transformarListaMensagemEmListaMensagemDTO(mensagemRepository.findByCdUsuarioRemetente(cdUsuarioRemetente));

    }

    public List<MensagemDTO> listarMensagensPorUsuarioDataEnvioDecrescente(int cdUsuarioRemetente){
        return transformarListaMensagemEmListaMensagemDTO(mensagemRepository.findByCdUsuarioRemetenteOrderByDataEnvioDesc(cdUsuarioRemetente));

    }

    private List<MensagemDTO> transformarListaMensagemEmListaMensagemDTO(List<Mensagem> lista){
        List<MensagemDTO> listaDTO = new ArrayList<>();
        for (Mensagem mensagem : lista){
            MensagemDTO mensagemDTO = new MensagemDTO();
            mensagemDTO.setConteudo(mensagem.getConteudo());
            mensagemDTO.setCdUsuarioDestinatario(mensagem.getCdUsuarioDestinatario());
            mensagemDTO.setCdUsuarioRemetente(mensagem.getCdUsuarioRemetente());
            listaDTO.add(mensagemDTO);
        }
        return listaDTO;
    }


}
