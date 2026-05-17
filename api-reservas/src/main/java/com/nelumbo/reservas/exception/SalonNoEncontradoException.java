package com.nelumbo.reservas.exception;

public class SalonNoEncontradoException extends RuntimeException {
    public SalonNoEncontradoException(Long id) {
        super("No existe un salón con id: " + id);
    }
}