package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;
import com.trocabook.api.chat.Chat.Model.MensagemBackup;
import com.trocabook.api.chat.Chat.Repository.MensagemBackupRepository;
import com.trocabook.api.chat.Chat.Repository.MensagemRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService implements IChatService{

    final MensagemRepository mensagemRepository;

    final MensagemBackupRepository mensagemBackupRepository;
    @Autowired
    public ChatService(MensagemRepository mensagemRepository, MensagemBackupRepository mensagemBackupRepository) {
        this.mensagemRepository = mensagemRepository;
        this.mensagemBackupRepository = mensagemBackupRepository;

    }

    public MensagemDTO enviarMensagem(MensagemDTO mensagemDTO){
        Mensagem mensagem = new Mensagem();
        mensagem.setCdUsuarioDestinatario(mensagemDTO.getCdUsuarioDestinatario());
        mensagem.setConteudo(mensagemDTO.getConteudo());
        mensagem.setCdUsuarioRemetente(mensagemDTO.getCdUsuarioRemetente());
        mensagem.setCdUsuarioLivro(mensagemDTO.getCdUsuarioLivro());
        mensagem.setDataEnvio(LocalDateTime.now());
        mensagemRepository.save(mensagem);
        mensagemDTO.setId(mensagem.getId().toHexString());
        return mensagemDTO;



    }

    public List<MensagemDTO> listarMensagensEntreUsuarios(int cdUsuarioLivro, int cdUsuarioRemetente, int cdUsuarioDestinatario) {
        return transformarListaMensagemEmListaMensagemDTO(
                mensagemRepository.findMensagensEntreUsuarios(cdUsuarioLivro, cdUsuarioRemetente, cdUsuarioDestinatario)
        );
    }


    public List<MensagemDTO> listarMensagensPorUsuario(int cdUsuarioRemetente){
        return transformarListaMensagemEmListaMensagemDTO(mensagemRepository.findByCdUsuarioRemetente(cdUsuarioRemetente));

    }

    public List<MensagemDTO> listarMensagensPorUsuarioDataEnvioDecrescente(int cdUsuarioRemetente){

        List<Mensagem> todasMensagens =
                mensagemRepository.findByCdUsuarioRemetenteOrCdUsuarioDestinatarioOrderByDataEnvioDesc(
                        cdUsuarioRemetente, cdUsuarioRemetente
                );


        Map<String, Mensagem> ultimaMensagemPorConversa = new LinkedHashMap<>();


        for (Mensagem m : todasMensagens) {

            int outroUsuario = (m.getCdUsuarioRemetente() == cdUsuarioRemetente)
                    ? m.getCdUsuarioDestinatario()
                    : m.getCdUsuarioRemetente();


            String chave = outroUsuario + "-" + m.getCdUsuarioLivro();


            ultimaMensagemPorConversa.putIfAbsent(chave, m);
        }


        return ultimaMensagemPorConversa
                .values()
                .stream()
                .map(MensagemDTO::new)
                .toList();
    }

    public boolean removerMensagem(String id) {
        ObjectId idObjeto = new ObjectId(id);
        Mensagem mensagem = mensagemRepository.findById(idObjeto).orElse(null);
        if (mensagem == null){
            return false;
        }
        MensagemBackup mensagemBackup = new MensagemBackup(mensagem, MensagemBackup.TipoBackup.EXCLUSAO);
        mensagemBackupRepository.save(mensagemBackup);
        mensagemRepository.deleteById(mensagem.getId());
        return true;
    }

    public Mensagem alterarMensagem(String id, String conteudo) {
        ObjectId idObjeto = new ObjectId(id);
        Mensagem mensagem = mensagemRepository.findById(idObjeto).orElse(null);
        if (mensagem == null){
            return null;
        }
        MensagemBackup mensagemBackup = new MensagemBackup(mensagem, MensagemBackup.TipoBackup.ALTERACAO);
        mensagemBackupRepository.save(mensagemBackup);
        mensagem.setConteudo(conteudo);
        return mensagemRepository.save(mensagem);
    }



    private List<MensagemDTO> transformarListaMensagemEmListaMensagemDTO(List<Mensagem> lista){
        List<MensagemDTO> listaDTO = new ArrayList<>();
        for (Mensagem mensagem : lista){
            MensagemDTO mensagemDTO = new MensagemDTO(mensagem);
            listaDTO.add(mensagemDTO);
        }
        return listaDTO;
    }




}
