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

    private final MensagemRepository mensagemRepository;

    private final MensagemBackupRepository mensagemBackupRepository;
    @Autowired
    public ChatService(MensagemRepository mensagemRepository, MensagemBackupRepository mensagemBackupRepository) {
        this.mensagemRepository = mensagemRepository;
        this.mensagemBackupRepository = mensagemBackupRepository;

    }

    @Override
    public MensagemDTO buscarPorId(String id) {
        Mensagem mensagem = mensagemRepository.findById(new ObjectId(id)).orElse(null);

        if (mensagem == null){
            return null;
        }

        return new MensagemDTO(mensagem);
    }

    @Override
    public MensagemDTO enviarMensagem(MensagemDTO mensagemDTO){
        Mensagem mensagem = new Mensagem();
        mensagem.setUidDestinatario(mensagemDTO.getUidDestinatario());
        mensagem.setConteudo(mensagemDTO.getConteudo());
        mensagem.setUidRemetente(mensagemDTO.getUidRemetente());
        mensagem.setUidAnuncio(mensagemDTO.getUidAnuncio());
        mensagem.setDataEnvio(LocalDateTime.now());
        mensagemRepository.save(mensagem);
        mensagemDTO.setId(mensagem.getId().toHexString());
        mensagemDTO.setDataEnvio(mensagem.getDataEnvio());
        return mensagemDTO;



    }

    @Override
    public List<MensagemDTO> listarMensagensEntreUsuarios(String uidAnuncio, String uidRemetente, String uidDestinatario) {
        return transformarListaMensagemEmListaMensagemDTO(
                mensagemRepository.findMensagensEntreUsuarios(uidAnuncio, uidRemetente, uidDestinatario)
        );
    }


    @Override
    public List<MensagemDTO> listarMensagensPorUsuario(String uidRemetente){
        return transformarListaMensagemEmListaMensagemDTO(mensagemRepository.findByUidRemetente(uidRemetente));

    }

    @Override
    public List<MensagemDTO> listarMensagensPorUsuarioDataEnvioDecrescente(String uidRemetente){

        List<Mensagem> todasMensagens =
                mensagemRepository.findByUidRemetenteOrUidDestinatarioOrderByDataEnvioDesc(
                        uidRemetente, uidRemetente
                );


        Map<String, Mensagem> ultimaMensagemPorConversa = new LinkedHashMap<>();


        for (Mensagem m : todasMensagens) {

            String outroUsuario = (m.getUidRemetente().equals(uidRemetente))
                    ? m.getUidDestinatario()
                    : m.getUidRemetente();


            String chave = outroUsuario + "-" + m.getUidAnuncio();


            ultimaMensagemPorConversa.putIfAbsent(chave, m);
        }


        return ultimaMensagemPorConversa
                .values()
                .stream()
                .map(MensagemDTO::new)
                .toList();
    }

    @Override
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

    @Override
    public MensagemDTO alterarMensagem(String id, String conteudo) {
        ObjectId idObjeto = new ObjectId(id);
        Mensagem mensagem = mensagemRepository.findById(idObjeto).orElse(null);
        if (mensagem == null){
            return null;
        }
        MensagemBackup mensagemBackup = new MensagemBackup(mensagem, MensagemBackup.TipoBackup.ALTERACAO);
        mensagemBackupRepository.save(mensagemBackup);
        mensagem.setConteudo(conteudo);
        return new MensagemDTO(mensagemRepository.save(mensagem));
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
