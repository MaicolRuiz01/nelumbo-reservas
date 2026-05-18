package com.nelumbo.reservas.controller;

import com.nelumbo.reservas.dto.request.FinalizarReservaRequest;
import com.nelumbo.reservas.dto.request.RechazarReservaRequest;
import com.nelumbo.reservas.dto.request.ReservaRequest;
import com.nelumbo.reservas.dto.response.CrearReservaResponse;
import com.nelumbo.reservas.dto.response.FinalizarReservaResponse;
import com.nelumbo.reservas.dto.response.ReservaResponse;
import com.nelumbo.reservas.service.ReservaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
@Validated
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<CrearReservaResponse> registrar(
            @Valid @RequestBody ReservaRequest request,
            Authentication auth) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservaService.registrar(request, auth.getName(), esAdmin(auth)));
    }

    @PostMapping("/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<FinalizarReservaResponse> finalizar(
            @Valid @RequestBody FinalizarReservaRequest request,
            Authentication auth) {

        return ResponseEntity.ok(
                reservaService.finalizar(request, auth.getName(), esAdmin(auth))
        );
    }

    @GetMapping("/salon/{salonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<List<ReservaResponse>> listarActivasPorSalon(
            @PathVariable Long salonId,
            Authentication auth) {

        return ResponseEntity.ok(
                reservaService.listarActivasPorSalon(salonId, auth.getName(), esAdmin(auth))
        );
    }


    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<List<ReservaResponse>> buscarPorDocumento(
            @RequestParam
            @NotBlank(message = "El parámetro 'documento' es obligatorio")
            @Pattern(
                    regexp = "^[0-9]+$",
                    message = "El parámetro 'documento' debe contener únicamente dígitos numéricos"
            )
            String documento,
            Authentication auth) {

        return ResponseEntity.ok(
                reservaService.buscarPorDocumentoParcial(documento, auth.getName(), esAdmin(auth))
        );
    }


    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.aprobar(id));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponse> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody RechazarReservaRequest request) {

        return ResponseEntity.ok(reservaService.rechazar(id, request));
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
