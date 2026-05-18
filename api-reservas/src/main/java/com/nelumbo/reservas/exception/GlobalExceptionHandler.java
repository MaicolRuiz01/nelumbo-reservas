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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

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

    @ExceptionHandler(GestorNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarGestorNoEncontrado(
            GestorNoEncontradoException ex, HttpServletRequest request) {
        log.warn("Gestor no encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.simple(ex.getMessage(), 404, request.getRequestURI()));
    }

    @ExceptionHandler(UsuarioNoEsGestorException.class)
    public ResponseEntity<ErrorResponse> manejarUsuarioNoEsGestor(
            UsuarioNoEsGestorException ex, HttpServletRequest request) {
        log.warn("Usuario no tiene rol GESTOR: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.simple(ex.getMessage(), 400, request.getRequestURI()));
    }

    @ExceptionHandler(SucursalNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejarSucursalNoEncontrada(
            SucursalNoEncontradaException ex, HttpServletRequest request) {
        log.warn("Sucursal no encontrada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.simple(ex.getMessage(), 404, request.getRequestURI()));
    }

    @ExceptionHandler(SalonNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarSalonNoEncontrado(
            SalonNoEncontradoException ex, HttpServletRequest request) {
        log.warn("Salón no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.simple(ex.getMessage(), 404, request.getRequestURI()));
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegadoNegocio(
            AccesoDenegadoException ex, HttpServletRequest request) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.simple(ex.getMessage(), 403, request.getRequestURI()));
    }

    @ExceptionHandler(SucursalConSalonesException.class)
    public ResponseEntity<ErrorResponse> manejarSucursalConSalones(
            SucursalConSalonesException ex, HttpServletRequest request) {
        log.warn("Eliminar sucursal con salones: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.simple(ex.getMessage(), 400, request.getRequestURI()));
    }

    @ExceptionHandler(ReservaInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarReservaInvalida(
            ReservaInvalidaException ex, HttpServletRequest request) {
        log.warn("Reserva inválida: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.simple(ex.getMessage(), 400, request.getRequestURI()));
    }

    @ExceptionHandler(ReservaActivaExistenteException.class)
    public ResponseEntity<ErrorResponse> manejarReservaActivaExistente(
            ReservaActivaExistenteException ex, HttpServletRequest request) {
        log.warn("Reserva activa duplicada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.simple(ex.getMessage(), 400, request.getRequestURI()));
    }

    @ExceptionHandler(CapacidadSalonExcedidaException.class)
    public ResponseEntity<ErrorResponse> manejarCapacidadExcedida(
            CapacidadSalonExcedidaException ex, HttpServletRequest request) {
        log.warn("Capacidad de salón excedida: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.simple(ex.getMessage(), 400, request.getRequestURI()));
    }

    @ExceptionHandler(ReservaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejarReservaNoEncontrada(
            ReservaNoEncontradaException ex, HttpServletRequest request) {
        if (ex.esErrorDeFinalizacion()) {
            log.warn("Finalizar reserva sin reserva activa: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.simple(ex.getMessage(), 400, request.getRequestURI()));
        }
        log.warn("Reserva no encontrada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.simple(ex.getMessage(), 404, request.getRequestURI()));
    }

    @ExceptionHandler(ReservaNoEsPendienteException.class)
    public ResponseEntity<ErrorResponse> manejarReservaNoPendiente(
            ReservaNoEsPendienteException ex, HttpServletRequest request) {
        log.warn("Intento de aprobar/rechazar reserva no pendiente: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.simple(ex.getMessage(), 400, request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<String> errores = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        log.warn("Errores de validación en parámetros: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.conErrores(
                        "Error de validacion en los parametros enviados",
                        400,
                        request.getRequestURI(),
                        errores));
    }

    /**
     * Se lanza cuando el cliente envia un body vacio, mal formado, o cuyo JSON no
     * coincide con el DTO esperado. Antes caia en el handler generico y devolvia 500;
     * el correcto es 400 porque el error es del cliente.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarBodyInvalido(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String detalle = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Body de la peticion ausente o mal formado: {}", detalle);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.simple(
                        "El body de la peticion es obligatorio y debe ser JSON valido",
                        400,
                        request.getRequestURI()));
    }

    /**
     * Se lanza cuando un @PathVariable o @RequestParam no se puede convertir al tipo
     * esperado (ej: GET /reservas/abc/aprobar cuando id es Long).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String mensaje = "El parametro '" + ex.getName() + "' tiene un tipo invalido. Se esperaba "
                + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "otro tipo");
        log.warn("Tipo de parametro invalido: {} = {}", ex.getName(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.simple(mensaje, 400, request.getRequestURI()));
    }

    private String formatearError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}