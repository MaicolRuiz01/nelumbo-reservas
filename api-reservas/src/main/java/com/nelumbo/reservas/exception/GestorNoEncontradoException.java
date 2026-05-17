package com.nelumbo.reservas.exception;

public class GestorNoEncontradoException extends RuntimeException {
    public GestorNoEncontradoException(Long id) {
        super("No existe un usuario con id: " + id);
    }
}
