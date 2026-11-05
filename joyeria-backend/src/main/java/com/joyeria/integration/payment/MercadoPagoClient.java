package com.joyeria.integration.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Cliente HTTP mínimo para la API de Mercado Pago (Checkout Pro).
 * Aísla las llamadas de red para poder sustituirlo en pruebas.
 */
@Component
@Slf4j
public class MercadoPagoClient {

    static final String BASE_URL = "https://api.mercadopago.com";

    private final RestClient restClient;

    public MercadoPagoClient() {
        this(RestClient.builder().baseUrl(BASE_URL).build());
    }

    /** Constructor para pruebas con un RestClient preconfigurado. */
    MercadoPagoClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Crea una preferencia de Checkout Pro y devuelve el JSON de respuesta. */
    public Map<String, Object> createPreference(String accessToken, Map<String, Object> body) {
        try {
            return restClient.post()
                    .uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException e) {
            log.error("Mercado Pago rechazó la preferencia: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentProcessingException("No se pudo iniciar el pago con Mercado Pago.", e);
        } catch (RuntimeException e) {
            log.error("Error de red al crear la preferencia de Mercado Pago", e);
            throw new PaymentProcessingException("No se pudo contactar con Mercado Pago.", e);
        }
    }

    /** Consulta el detalle de un pago por su id. */
    public Map<String, Object> getPayment(String accessToken, String paymentId) {
        try {
            return restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException e) {
            log.error("Mercado Pago rechazó la consulta del pago {}: {} {}", paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentProcessingException("No se pudo verificar el pago con Mercado Pago.", e);
        } catch (RuntimeException e) {
            log.error("Error de red al consultar el pago {} de Mercado Pago", paymentId, e);
            throw new PaymentProcessingException("No se pudo contactar con Mercado Pago.", e);
        }
    }
}
