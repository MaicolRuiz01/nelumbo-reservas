package com.nelumbo.reservas.exception;

import com.nelumbo.reservas.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<ErrorResponse> manejarEmailYaRegistrado(
            EmailYaRegistradoException ex, HttpServletRequest request) {
        log.warn("Email duplicado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.simple(ex.getMessage(), 400, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatearError)
                .toList();
        log.warn("Errores de validacion: {}", errores);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.conErrores(
                        "Error de validacion en los campos enviados",
                        400,
                        request.getRequestURI(),
                        errores));
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> manejarCredencialesInvalidas(
            Exception ex, HttpServletRequest request) {
        log.warn("Credenciales invalidas: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.simple(
                        "Credenciales invalidas",
                        401,
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado a: {}", request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.simple(
                        "No tienes permisos para realizar esta accion",
                        403,
                        request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> manejarAutenticacion(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Error de autenticacion: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.simple(
                        "Autenticacion requerida",
                        401,
                        request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGenerico(
            Exception ex, HttpServletRequest request) {
        log.error("Error no controlado: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.simple(
                        "Error interno del servidor",
                        500,
                        request.getRequestURI()));
    }

    private String formatearError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}