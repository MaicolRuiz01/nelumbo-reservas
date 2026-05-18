package com.nelumbo.reservas.exception;

public class ReservaActivaExistenteException extends RuntimeException {

    public ReservaActivaExistenteException() {
        super("No se puede Registrar Reserva, ya existe una reserva activa para este documento en este u otro salón");
    }
}
