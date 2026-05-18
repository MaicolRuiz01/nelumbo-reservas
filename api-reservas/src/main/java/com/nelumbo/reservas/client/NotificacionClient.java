package com.nelumbo.reservas.client;

import com.nelumbo.reservas.client.dto.NotificacionMicroservicioRequest;
import com.nelumbo.reservas.client.dto.NotificacionMicroservicioResponse;
import com.nelumbo.reservas.exception.NotificacionMicroservicioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionClient {

    private final @Qualifier("notificacionesWebClient") WebClient webClient;

    public NotificacionMicroservicioResponse enviar(NotificacionMicroservicioRequest request) {
        try {
            return webClient.post()
                    .uri("/notificaciones")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new NotificacionMicroservicioException(
                                            "El microservicio de notificaciones respondió con error " +
                                                    response.statusCode() + ": " + body))
                    )
                    .bodyToMono(NotificacionMicroservicioResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Error HTTP al llamar al microservicio de notificaciones: {}", ex.getMessage(), ex);
            throw new NotificacionMicroservicioException(
                    "Fallo al comunicarse con el microservicio de notificaciones", ex);
        } catch (NotificacionMicroservicioException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error inesperado al llamar al microservicio de notificaciones", ex);
            throw new NotificacionMicroservicioException(
                    "El microservicio de notificaciones no está disponible", ex);
        }
    }
}
