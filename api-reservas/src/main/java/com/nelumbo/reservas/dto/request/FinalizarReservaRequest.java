package com.nelumbo.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record FinalizarReservaRequest(

        @NotBlank(message = "El documento del cliente es obligatorio")
        @Pattern(
                regexp = "^[0-9]{6,12}$",
                message = "El documento del cliente debe contener entre 6 y 12 dígitos numéricos"
        )
        String documentoCliente,

        @NotNull(message = "El id del salón es obligatorio")
        Long salonId
) {
}
