package com.nelumbo.reservas.exception;

public class ReservaInvalidaException extends RuntimeException {
    public ReservaInvalidaException() {
        super("La fecha de fin estimada debe ser posterior a la fecha de inicio");
    }
}
