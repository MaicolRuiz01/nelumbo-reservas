package com.nelumbo.reservas.exception;


public class ReservaNoEncontradaException extends RuntimeException {

    private final boolean errorDeFinalizacion;

    public ReservaNoEncontradaException() {
        super("No se puede Finalizar Reserva, no existe una reserva activa para este documento en el salón");
        this.errorDeFinalizacion = true;
    }


    public ReservaNoEncontradaException(Long id) {
        super("Reserva no encontrada con id: " + id);
        this.errorDeFinalizacion = false;
    }

    public boolean esErrorDeFinalizacion() {
        return errorDeFinalizacion;
    }
}
