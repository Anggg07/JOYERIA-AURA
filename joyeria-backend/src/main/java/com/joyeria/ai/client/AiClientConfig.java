package com.joyeria.ai.client;

import com.joyeria.ai.AiPromptService;
import com.joyeria.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el cliente de IA. Si no hay proveedor configurado, se registra un
 * cliente inactivo y el asistente funciona en modo determinista.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
@Slf4j
public class AiClientConfig {

    @Bean
    public AiChatClient aiChatClient(AiProperties properties, AiPromptService promptService) {
        if (properties.isConfigured()) {
            log.info("Asistente IA habilitado (proveedor={}, modelo={})", properties.getProvider(), properties.getModel());
            return new OpenAiCompatibleChatClient(properties, promptService);
        }
        log.info("Asistente IA sin proveedor externo: modo determinista activo.");
        return new DisabledAiChatClient(properties);
    }
}
