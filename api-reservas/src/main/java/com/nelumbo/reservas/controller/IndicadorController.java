package com.nelumbo.reservas.controller;

import com.nelumbo.reservas.dto.response.GananciasResponse;
import com.nelumbo.reservas.dto.response.ReservaResponse;
import com.nelumbo.reservas.dto.response.TopClienteResponse;
import com.nelumbo.reservas.dto.response.TopSucursalResponse;
import com.nelumbo.reservas.service.IndicadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/top-clientes/salon/{salonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<List<TopClienteResponse>> topClientesPorSalon(
            @PathVariable Long salonId,
            Authentication auth
    ) {

        return ResponseEntity.ok(
                indicadorService.topClientesPorSalon(
                        salonId,
                        auth.getName(),
                        esAdmin(auth)
                )
        );
    }

    @GetMapping("/clientes-primera-vez/salon/{salonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<List<ReservaResponse>> clientesPrimeraVezPorSalon(
            @PathVariable Long salonId,
            Authentication auth
    ) {

        return ResponseEntity.ok(
                indicadorService.clientesPrimeraVezPorSalon(
                        salonId,
                        auth.getName(),
                        esAdmin(auth)
                )
        );
    }

    @GetMapping("/ganancias/salon/{salonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<GananciasResponse> gananciasPorSalon(
            @PathVariable Long salonId,
            Authentication auth
    ) {

        return ResponseEntity.ok(
                indicadorService.gananciasPorSalon(
                        salonId,
                        auth.getName(),
                        esAdmin(auth)
                )
        );
    }

    @GetMapping("/top-sucursales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TopSucursalResponse>> topSucursalesMesActual() {

        return ResponseEntity.ok(
                indicadorService.topSucursalesMesActual()
        );
    }
}