package com.trocabook.api.chat.Chat.Repository;

import com.trocabook.api.chat.Chat.Model.Mensagem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MensagemRepository extends MongoRepository<Mensagem, String> {
    List<Mensagem> findByCdUsuarioRemetenteAndCdUsuarioDestinatarioOrCdUsuarioDestinatarioAndCdUsuarioRemetenteOrderByDataEnvioAsc
            (int cdUsuarioRemetente1, int cdUsuarioDestinatario1, int cdUsuarioDestinatario2, int cdUsuarioRemetente2);

    List<Mensagem> findByCdUsuarioRemetente(int cdUsuarioRemetente);

    List<Mensagem> findByCdUsuarioRemetenteOrderByDataEnvioDesc(int cdUsuarioRemetente);


}
