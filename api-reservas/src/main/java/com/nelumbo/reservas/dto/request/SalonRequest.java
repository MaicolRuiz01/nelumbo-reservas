package com.nelumbo.reservas.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SalonRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotNull(message = "La capacidad es obligatoria")
        @Positive(message = "La capacidad debe ser un número positivo")
        Integer capacidad,

        @NotNull(message = "El costo por hora es obligatorio")
        @DecimalMin(value = "0.01", message = "El costo por hora debe ser mayor a cero")
        BigDecimal costoPorHora,

        @NotNull(message = "El id de la sucursal es obligatorio")
        Long sucursalId

) {}