package com.nelumbo.reservas.exception;

public class CapacidadSalonExcedidaException extends RuntimeException {

    public CapacidadSalonExcedidaException() {
        super("No se puede Registrar Reserva, capacidad insuficiente en el salón");
    }
}
