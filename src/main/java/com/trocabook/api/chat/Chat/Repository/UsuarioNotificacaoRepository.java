package com.trocabook.api.chat.Chat.Repository;

import com.trocabook.api.chat.Chat.Model.UsuarioNotificacao;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UsuarioNotificacaoRepository extends MongoRepository<UsuarioNotificacao, String> {
    Optional<UsuarioNotificacao> findByUidUsuario(String uidUsuario);
}