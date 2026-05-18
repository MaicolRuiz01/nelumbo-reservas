package com.nelumbo.microservicionotificaciones.service;

import com.nelumbo.microservicionotificaciones.dto.request.NotificacionRequest;
import com.nelumbo.microservicionotificaciones.dto.response.NotificacionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {
    public NotificacionResponse procesar(NotificacionRequest request) {
        log.info("Notificación recibida → email={}, documento={}, salonNombre={}, mensaje={}",
                request.email(),
                request.documento(),
                request.salonNombre(),
                request.mensaje());

        return new NotificacionResponse("Notificación Enviada");
    }
}
