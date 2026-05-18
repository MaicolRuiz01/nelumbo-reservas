package com.nelumbo.reservas.dto.response;

import com.nelumbo.reservas.enums.EstadoReserva;

import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,

        String documentoCliente,

        String nombreCliente,

        LocalDateTime fechaInicio,

        LocalDateTime fechaFinEstimada,

        LocalDateTime fechaCreacion,

        Integer asistentes,

        EstadoReserva estado,

        String motivoRechazo,

        Long salonId,

        String salonNombre,

        Long gestorId,

        String gestorNombre

) {
}
