package com.nelumbo.reservas.dto.response;

import java.math.BigDecimal;

public record SalonResponse(
        Long id,
        String nombre,
        Integer capacidad,
        BigDecimal costoPorHora,
        Long sucursalId,
        String sucursalNombre,
        Long gestorId,
        String gestorNombre
) {
}
