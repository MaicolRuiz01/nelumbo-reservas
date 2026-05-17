package com.nelumbo.reservas.exception;

public class UsuarioNoEsGestorException extends RuntimeException {
    public UsuarioNoEsGestorException(Long id) {
        super("El usuario con id " + id + " no tiene rol GESTOR");
    }
}
