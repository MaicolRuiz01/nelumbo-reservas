package com.nelumbo.reservas.dto.response;

import java.math.BigDecimal;

public record FinalizarReservaResponse(
        String mensaje,
        BigDecimal totalCobrado
) {
}
