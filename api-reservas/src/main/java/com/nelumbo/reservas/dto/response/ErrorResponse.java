package com.nelumbo.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String mensaje,
        Integer status,
        LocalDateTime timestamp,
        String path,
        List<String> errores
) {
    public static ErrorResponse simple(String mensaje, int status, String path) {
        return new ErrorResponse(mensaje, status, LocalDateTime.now(), path, null);
    }

    public static ErrorResponse conErrores(String mensaje, int status, String path, List<String> errores) {
        return new ErrorResponse(mensaje, status, LocalDateTime.now(), path, errores);
    }
}