package com.nelumbo.reservas.dto.response;

public record TopClienteResponse(
        String documentoCliente,
        String nombreCliente,
        Long cantidad
) {
}
