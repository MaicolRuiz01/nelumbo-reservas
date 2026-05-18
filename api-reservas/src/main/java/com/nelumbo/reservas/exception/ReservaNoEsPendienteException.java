package com.nelumbo.reservas.exception;


public class ReservaNoEsPendienteException extends RuntimeException {

    public ReservaNoEsPendienteException() {
        super("Solo se pueden aprobar o rechazar reservas en estado PENDIENTE_APROBACION");
    }
}
