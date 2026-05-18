package com.nelumbo.reservas.dto.response;

import java.math.BigDecimal;

public record GananciasResponse(
        BigDecimal hoy,
        BigDecimal semana,
        BigDecimal mes,
        BigDecimal anio
) {
}
