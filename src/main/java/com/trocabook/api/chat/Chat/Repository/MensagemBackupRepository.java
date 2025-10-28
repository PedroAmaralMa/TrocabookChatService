package com.trocabook.api.chat.Chat.Repository;

import com.trocabook.api.chat.Chat.Model.MensagemBackup;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MensagemBackupRepository extends MongoRepository<MensagemBackup, ObjectId> {
}
