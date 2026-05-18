package com.nelumbo.reservas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record NotificacionEnviarRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        String email,

        @NotBlank(message = "El documento es obligatorio")
        @Pattern(regexp = "^[0-9]{6,12}$",
                message = "El documento debe tener entre 6 y 12 dígitos numéricos")
        String documento,

        @NotBlank(message = "El mensaje es obligatorio")
        String mensaje,

        @NotNull(message = "El salonId es obligatorio")
        @Positive(message = "El salonId debe ser positivo")
        Long salonId
) {
}