package com.nelumbo.reservas.dto.response;

import com.nelumbo.reservas.enums.Rol;

public record AuthResponse(
        String token,
        String tipo,
        String email,
        String nombre,
        Rol rol
) {
    public static AuthResponse bearer(String token, String email, String nombre, Rol rol) {
        return new AuthResponse(token, "Bearer", email, nombre, rol);
    }
}
