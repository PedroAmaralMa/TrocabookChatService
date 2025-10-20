package com.trocabook.api.chat.Chat.Repository;

import com.trocabook.api.chat.Chat.Model.Mensagem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MensagemRepository extends MongoRepository<Mensagem, String> {
    @Query("{ 'cdUsuarioLivro': ?0, $or: [ " +
            "{'cdUsuarioRemetente': ?1, 'cdUsuarioDestinatario': ?2}, " +
            "{'cdUsuarioRemetente': ?2, 'cdUsuarioDestinatario': ?1} " +
            "] }")
    List<Mensagem> findMensagensEntreUsuarios(int cdUsuarioLivro, int remetente, int destinatario);




    List<Mensagem> findByCdUsuarioRemetente(int cdUsuarioRemetente);

    List<Mensagem> findByCdUsuarioRemetenteOrCdUsuarioDestinatarioOrderByDataEnvioDesc(int cdUsuarioRemetente, int cdUsuarioDestinatario);


}
