package com.trocabook.api.chat.Chat;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatApplication {

    private static final Logger log = LoggerFactory.getLogger(ChatApplication.class);

    public static void main(String[] args) {
        log.info("Iniciando TrocabookChatService (ChatApplication)...");
        carregarTodasVariaveis();
        SpringApplication.run(ChatApplication.class, args);
        log.info("TrocabookChatService iniciado com sucesso!");
    }

    public static void carregarTodasVariaveis() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();


        for (DotenvEntry entry : dotenv.entries()) {
            String chave = entry.getKey();
            String valor = entry.getValue();

            System.setProperty(chave, valor);
            log.debug("[ENV] Variável de ambiente carregada: {}", chave);
        }
    }

}
