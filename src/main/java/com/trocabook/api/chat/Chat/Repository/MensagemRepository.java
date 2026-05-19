package com.trocabook.api.chat.Chat.Repository;

import com.trocabook.api.chat.Chat.Model.Mensagem;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MensagemRepository extends MongoRepository<Mensagem, ObjectId> {
    @Query("{ 'uidAnuncio': ?0, $or: [ " +
            "{'uidRemetente': ?1, 'uidDestinatario': ?2}, " +
            "{'uidRemetente': ?2, 'uidDestinatario': ?1} " +
            "] }")
    List<Mensagem> findMensagensEntreUsuarios(String uidAnuncio, String uidRemetente, String uidDestinatario);




    List<Mensagem> findByUidRemetente(String uidRemetente);

    List<Mensagem> findByUidRemetenteOrUidDestinatarioOrderByDataEnvioDesc(String uidRemetente, String uidDestinatario);


}
