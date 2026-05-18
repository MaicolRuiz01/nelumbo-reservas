package com.nelumbo.reservas.dto.response;

import java.math.BigDecimal;

public record TopSucursalResponse(
        Long sucursalId,
        String sucursalNombre,
        BigDecimal totalFacturado
) {
}
