package com.joyeria.ai.admin.client;

import com.joyeria.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el cliente de IA del asistente de administración. Resuelve su
 * configuración a partir de {@code ai.admin.*} y, cuando algún valor no se
 * define, hereda el de {@code ai.*}. Sin proveedor disponible se registra un
 * cliente inactivo y el asistente cae en modo determinista.
 */
@Configuration
@Slf4j
public class AiAdminClientConfig {

    @Bean
    public AiAdminChatClient aiAdminChatClient(AiProperties properties) {
        AiProperties.Admin admin = properties.getAdmin();
        String apiKey = firstNonBlank(admin.getApiKey(), properties.getApiKey());
        String baseUrl = firstNonBlank(admin.getBaseUrl(), properties.getBaseUrl());
        String model = firstNonBlank(admin.getModel(), properties.getModel());
        int timeout = admin.getTimeoutSeconds() > 0 ? admin.getTimeoutSeconds() : properties.getTimeoutSeconds();

        boolean configured = admin.isEnabled()
                && !apiKey.isBlank()
                && !baseUrl.isBlank()
                && !model.isBlank();

        if (configured) {
            log.info("Asistente de administración habilitado (modelo={}, proveedorAislado={}).",
                    model, !admin.getApiKey().isBlank());
            return new OpenAiCompatibleAdminChatClient(baseUrl, apiKey, model, admin.getTemperature(), timeout);
        }
        log.info("Asistente de administración sin proveedor externo: modo determinista activo.");
        return new DisabledAdminChatClient();
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }
}
