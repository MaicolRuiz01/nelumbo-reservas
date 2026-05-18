package com.nelumbo.reservas.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record ReservaRequest(

        @NotBlank(message = "El documento del cliente es obligatorio")
        @Pattern(
                regexp = "^[0-9]{6,12}$",
                message = "El documento del cliente debe contener entre 6 y 12 dígitos numéricos"
        )
        String documentoCliente,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 150, message = "El nombre del cliente no puede superar 150 caracteres")
        String nombreCliente,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime fechaInicio,

        @NotNull(message = "La fecha de fin estimada es obligatoria")
        LocalDateTime fechaFinEstimada,

        @NotNull(message = "La cantidad de asistentes es obligatoria")
        @Positive(message = "La cantidad de asistentes debe ser mayor a cero")
        Integer asistentes,

        @NotNull(message = "El id del salón es obligatorio")
        Long salonId
) {


}
