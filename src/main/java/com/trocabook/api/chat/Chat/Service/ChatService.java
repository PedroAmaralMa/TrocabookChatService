package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;
import com.trocabook.api.chat.Chat.Model.MensagemBackup;
import com.trocabook.api.chat.Chat.Repository.MensagemBackupRepository;
import com.trocabook.api.chat.Chat.Repository.MensagemRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService implements IChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final MensagemRepository mensagemRepository;
    private final MensagemBackupRepository mensagemBackupRepository;

    @Autowired
    public ChatService(MensagemRepository mensagemRepository, MensagemBackupRepository mensagemBackupRepository) {
        this.mensagemRepository = mensagemRepository;
        this.mensagemBackupRepository = mensagemBackupRepository;
    }

    @Override
    public MensagemDTO buscarPorId(String id) {
        log.info("Buscando mensagem no MongoDB por ID: '{}'", id);
        Mensagem mensagem = mensagemRepository.findById(new ObjectId(id)).orElse(null);

        if (mensagem == null) {
            log.warn("Mensagem com ID '{}' não foi encontrada no MongoDB.", id);
            return null;
        }

        log.info("Mensagem com ID '{}' encontrada com sucesso.", id);
        return new MensagemDTO(mensagem);
    }

    @Override
    public MensagemDTO enviarMensagem(MensagemDTO mensagemDTO) {
        log.info("Iniciando persistência de mensagem no MongoDB. Remetente: '{}', Destinatário: '{}', Anúncio: '{}'",
                mensagemDTO.getUidRemetente(), mensagemDTO.getUidDestinatario(), mensagemDTO.getUidAnuncio());

        Mensagem mensagem = new Mensagem();
        mensagem.setUidDestinatario(mensagemDTO.getUidDestinatario());
        mensagem.setConteudo(mensagemDTO.getConteudo());
        mensagem.setUidRemetente(mensagemDTO.getUidRemetente());
        mensagem.setUidAnuncio(mensagemDTO.getUidAnuncio());
        mensagem.setDataEnvio(LocalDateTime.now());

        mensagemRepository.save(mensagem);

        String idGerado = mensagem.getId().toHexString();
        log.info("Mensagem persistida com sucesso no MongoDB com ID: '{}'", idGerado);

        mensagemDTO.setId(idGerado);
        mensagemDTO.setDataEnvio(mensagem.getDataEnvio());
        return mensagemDTO;
    }

    @Override
    public List<MensagemDTO> listarMensagensEntreUsuarios(String uidAnuncio, String uidRemetente, String uidDestinatario) {
        log.info("Buscando histórico de mensagens entre remetente '{}' e destinatário '{}' no anúncio '{}'",
                uidRemetente, uidDestinatario, uidAnuncio);

        List<MensagemDTO> mensagens = transformarListaMensagemEmListaMensagemDTO(
                mensagemRepository.findMensagensEntreUsuarios(uidAnuncio, uidRemetente, uidDestinatario)
        );

        log.info("Total de {} mensagens encontradas entre '{}' e '{}' para o anúncio '{}'",
                mensagens.size(), uidRemetente, uidDestinatario, uidAnuncio);
        return mensagens;
    }

    @Override
    public List<MensagemDTO> listarMensagensPorUsuario(String uidRemetente) {
        log.info("Buscando mensagens no MongoDB enviadas pelo remetente: '{}'", uidRemetente);
        List<MensagemDTO> mensagens = transformarListaMensagemEmListaMensagemDTO(mensagemRepository.findByUidRemetente(uidRemetente));
        log.info("Total de {} mensagens encontradas para o remetente '{}'", mensagens.size(), uidRemetente);
        return mensagens;
    }

    @Override
    public List<MensagemDTO> listarMensagensPorUsuarioDataEnvioDecrescente(String uidRemetente) {
        log.info("Buscando lista de conversas mais recentes para o usuário: '{}'", uidRemetente);

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

        List<MensagemDTO> resultado = ultimaMensagemPorConversa
                .values()
                .stream()
                .map(MensagemDTO::new)
                .toList();

        log.info("Total de {} conversas distintas encontradas para o usuário '{}'", resultado.size(), uidRemetente);
        return resultado;
    }

    @Override
    public boolean removerMensagem(String id) {
        log.info("Iniciando processo de remoção da mensagem ID: '{}'", id);
        ObjectId idObjeto = new ObjectId(id);
        Mensagem mensagem = mensagemRepository.findById(idObjeto).orElse(null);
        if (mensagem == null) {
            log.warn("Mensagem ID '{}' não encontrada no MongoDB para exclusão.", id);
            return false;
        }

        MensagemBackup mensagemBackup = new MensagemBackup(mensagem, MensagemBackup.TipoBackup.EXCLUSAO);
        mensagemBackupRepository.save(mensagemBackup);
        log.info("Backup de exclusão da mensagem '{}' salvo com sucesso.", id);

        mensagemRepository.deleteById(mensagem.getId());
        log.info("Mensagem ID '{}' excluída com sucesso do MongoDB.", id);
        return true;
    }

    @Override
    public MensagemDTO alterarMensagem(String id, String conteudo) {
        log.info("Iniciando processo de alteração da mensagem ID: '{}'", id);
        ObjectId idObjeto = new ObjectId(id);
        Mensagem mensagem = mensagemRepository.findById(idObjeto).orElse(null);
        if (mensagem == null) {
            log.warn("Mensagem ID '{}' não encontrada no MongoDB para alteração.", id);
            return null;
        }

        MensagemBackup mensagemBackup = new MensagemBackup(mensagem, MensagemBackup.TipoBackup.ALTERACAO);
        mensagemBackupRepository.save(mensagemBackup);
        log.info("Backup da mensagem original '{}' salvo antes da alteração.", id);

        mensagem.setConteudo(conteudo);
        Mensagem mensagemSalva = mensagemRepository.save(mensagem);
        log.info("Mensagem ID '{}' alterada com sucesso no MongoDB.", id);

        return new MensagemDTO(mensagemSalva);
    }

    private List<MensagemDTO> transformarListaMensagemEmListaMensagemDTO(List<Mensagem> lista) {
        List<MensagemDTO> listaDTO = new ArrayList<>();
        for (Mensagem mensagem : lista) {
            MensagemDTO mensagemDTO = new MensagemDTO(mensagem);
            listaDTO.add(mensagemDTO);
        }
        return listaDTO;
    }
}
