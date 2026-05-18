package com.nelumbo.reservas.controller;

import com.nelumbo.reservas.dto.request.NotificacionEnviarRequest;
import com.nelumbo.reservas.dto.response.NotificacionEnviadaResponse;
import com.nelumbo.reservas.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<NotificacionEnviadaResponse> enviar(
            @Valid @RequestBody NotificacionEnviarRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        boolean esAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        NotificacionEnviadaResponse response =
                notificacionService.enviar(request, email, esAdmin);

        return ResponseEntity.ok(response);
    }
}