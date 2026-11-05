package com.trocabook.api.chat.Chat.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_CREDENTIALS_PATH:${firebase.credentials.path:}}")
    private String caminhoCredencial;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (caminhoCredencial == null || caminhoCredencial.isBlank()) {
            throw new IllegalStateException(
                    "A propriedade 'FIREBASE_CREDENTIALS_PATH' ou 'firebase.credentials.path' não foi definida no .env ou application.properties."
            );
        }

        InputStream serviceAccount;

        if (caminhoCredencial.startsWith("classpath:")) {
            String pathLimpo = caminhoCredencial.replace("classpath:", "");
            if (pathLimpo.startsWith("/")) {
                pathLimpo = pathLimpo.substring(1);
            }
            serviceAccount = new ClassPathResource(pathLimpo).getInputStream();
        } else {
            serviceAccount = new FileInputStream(caminhoCredencial);
        }

        try (serviceAccount) {
            FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }

            return FirebaseApp.initializeApp(firebaseOptions);
        }
    }
}