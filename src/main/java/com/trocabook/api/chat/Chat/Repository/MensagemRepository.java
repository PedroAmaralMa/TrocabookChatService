package com.trocabook.api.chat.Chat.Repository;

import com.trocabook.api.chat.Chat.Model.Mensagem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MensagemRepository extends MongoRepository<Mensagem, String> {
    List<Mensagem> findByCdUsuarioRemetenteAndCdUsuarioDestinatarioOrCdUsuarioDestinatarioAndCdUsuarioRemetente
            (int cdUsuarioRemetente1, int cdUsuarioDestinatario1, int cdUsuarioDestinatario2, int cdUsarioRemetente2);

    List<Mensagem> findByCdUsuarioRemetente(int cdUsuarioRemetente);


}
