package com.nelumbo.reservas.controller;

import com.nelumbo.reservas.dto.response.TopClienteResponse;
import com.nelumbo.reservas.service.IndicadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/indicadores")
@RequiredArgsConstructor
public class IndicadorController {

    private final IndicadorService indicadorService;

    @GetMapping("/top-clientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<List<TopClienteResponse>> topClientesGlobal() {
        return ResponseEntity.ok(indicadorService.topClientesGlobal());
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}