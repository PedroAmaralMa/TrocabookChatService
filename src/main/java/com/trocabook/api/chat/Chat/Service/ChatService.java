package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;
import com.trocabook.api.chat.Chat.Repository.MensagemRepository;
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

    public List<MensagemDTO> listarMensagensEntreUsuarios(int cdUsuarioLivro, int cdUsuarioRemetente, int cdUsuarioDestinatario) {
        return transformarListaMensagemEmListaMensagemDTO(
                mensagemRepository.findMensagensEntreUsuarios(cdUsuarioLivro, cdUsuarioRemetente, cdUsuarioDestinatario)
        );
    }


    public List<MensagemDTO> listarMensagensPorUsuario(int cdUsuarioRemetente){
        return transformarListaMensagemEmListaMensagemDTO(mensagemRepository.findByCdUsuarioRemetente(cdUsuarioRemetente));

    }

    public List<MensagemDTO> listarMensagensPorUsuarioDataEnvioDecrescente(int cdUsuarioRemetente){
        // 1️⃣ Busca todas as mensagens em que o usuário participou
        List<Mensagem> todasMensagens =
                mensagemRepository.findByCdUsuarioRemetenteOrCdUsuarioDestinatarioOrderByDataEnvioDesc(
                        cdUsuarioRemetente, cdUsuarioRemetente
                );

        // 2️⃣ Cria um mapa para garantir que só pegaremos uma mensagem por conversa
        Map<String, Mensagem> ultimaMensagemPorConversa = new LinkedHashMap<>();

        // 3️⃣ Percorre todas as mensagens (já ordenadas da mais nova pra mais velha)
        for (Mensagem m : todasMensagens) {
            // Identifica o outro usuário envolvido na conversa
            int outroUsuario = (m.getCdUsuarioRemetente() == cdUsuarioRemetente)
                    ? m.getCdUsuarioDestinatario()
                    : m.getCdUsuarioRemetente();

            // Cria uma chave única pra identificar a conversa (livro + outro usuário)
            String chave = outroUsuario + "-" + m.getCdUsuarioLivro();

            // 4️⃣ Se ainda não existe no mapa, adiciona (a primeira é a mais recente)
            ultimaMensagemPorConversa.putIfAbsent(chave, m);
        }

        // 5️⃣ Converte os valores do mapa (as últimas mensagens) em DTOs
        return ultimaMensagemPorConversa
                .values()
                .stream()
                .map(MensagemDTO::new)
                .toList();
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
