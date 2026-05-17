package com.nelumbo.reservas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmailYaRegistradoException extends RuntimeException{

    public EmailYaRegistradoException(String email) {
        super("Ya existe un usuario registrado con el email: " + email);
    }
}
