package com.nelumbo.reservas.dto.response;

public record SucursalResponse(
        Long id,
        String nombre,
        String ciudad,
        String direccion,
        Long gestorId,
        String gestorNombre
) {
}
