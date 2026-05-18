package com.nelumbo.reservas.client.dto;

public record NotificacionMicroservicioRequest(
        String email,
        String documento,
        String mensaje,
        String salonNombre
) {
}
