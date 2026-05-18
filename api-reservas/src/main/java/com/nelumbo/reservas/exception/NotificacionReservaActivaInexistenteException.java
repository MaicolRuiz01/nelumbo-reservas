package com.nelumbo.reservas.exception;

public class NotificacionReservaActivaInexistenteException extends RuntimeException {

    public NotificacionReservaActivaInexistenteException(String mensaje) {
        super(mensaje);
    }
}