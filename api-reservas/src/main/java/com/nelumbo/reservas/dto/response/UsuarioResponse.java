package com.nelumbo.reservas.dto.response;

import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.Rol;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String email,
        String nombre,
        Rol rol,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getFechaCreacion()
        );
    }
}
