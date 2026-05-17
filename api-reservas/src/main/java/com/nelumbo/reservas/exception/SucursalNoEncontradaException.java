package com.nelumbo.reservas.exception;

public class SucursalNoEncontradaException extends RuntimeException {
    public SucursalNoEncontradaException(Long id) {
        super("No existe una sucursal con id: " + id);
    }
}