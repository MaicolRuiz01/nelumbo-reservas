package com.nelumbo.microservicionotificaciones.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record NotificacionRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        String email,

        @NotBlank(message = "El documento es obligatorio")
        String documento,

        @NotBlank(message = "El mensaje es obligatorio")
        String mensaje,

        @NotBlank(message = "El nombre del salón es obligatorio")
        String salonNombre
) {
}
