package com.nelumbo.microservicionotificaciones.controller;

import com.nelumbo.microservicionotificaciones.dto.request.NotificacionRequest;
import com.nelumbo.microservicionotificaciones.dto.response.NotificacionResponse;
import com.nelumbo.microservicionotificaciones.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {
    private final NotificacionService notificacionService;

    @PostMapping
    public ResponseEntity<NotificacionResponse> recibir(@Valid @RequestBody NotificacionRequest request) {
        return ResponseEntity.ok(notificacionService.procesar(request));
    }
}
