package com.nelumbo.reservas.exception;

public class NotificacionMicroservicioException extends RuntimeException {

    public NotificacionMicroservicioException(String mensaje) {
        super(mensaje);
    }

    public NotificacionMicroservicioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}