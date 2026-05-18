package com.nelumbo.reservas.service;

import com.nelumbo.reservas.client.NotificacionClient;
import com.nelumbo.reservas.client.dto.NotificacionMicroservicioRequest;
import com.nelumbo.reservas.client.dto.NotificacionMicroservicioResponse;
import com.nelumbo.reservas.dto.request.NotificacionEnviarRequest;
import com.nelumbo.reservas.dto.response.NotificacionEnviadaResponse;
import com.nelumbo.reservas.entity.Salon;
import com.nelumbo.reservas.enums.EstadoReserva;
import com.nelumbo.reservas.exception.AccesoDenegadoException;
import com.nelumbo.reservas.exception.NotificacionMicroservicioException;
import com.nelumbo.reservas.exception.NotificacionReservaActivaInexistenteException;
import com.nelumbo.reservas.exception.SalonNoEncontradoException;
import com.nelumbo.reservas.repository.ReservaRepository;
import com.nelumbo.reservas.repository.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final SalonRepository salonRepository;
    private final ReservaRepository reservaRepository;
    private final NotificacionClient notificacionClient;

    @Transactional(readOnly = true)
    public NotificacionEnviadaResponse enviar(NotificacionEnviarRequest request,
                                              String emailUsuarioAutenticado,
                                              boolean esAdmin) {

        // 1. Salón debe existir
        Salon salon = salonRepository.findById(request.salonId())
                .orElseThrow(() -> new SalonNoEncontradoException(request.salonId()));

        // 2. Si es GESTOR, el salón debe ser suyo
        if (!esAdmin && !salon.getGestor().getEmail().equals(emailUsuarioAutenticado)) {
            throw new AccesoDenegadoException(
                    "No tiene permisos para enviar notificaciones sobre este salón");
        }

        // 3. Debe existir una reserva ACTIVA para ese documento en ese salón
        reservaRepository.findByDocumentoClienteAndSalonIdAndEstado(
                        request.documento(), salon.getId(), EstadoReserva.ACTIVA)
                .orElseThrow(() -> new NotificacionReservaActivaInexistenteException(
                        "No se puede Enviar Notificación, no existe una reserva activa para este documento en el salón indicado"));

        // 4. Construir request para el microservicio (salonId → salonNombre)
        NotificacionMicroservicioRequest microRequest = new NotificacionMicroservicioRequest(
                request.email(),
                request.documento(),
                request.mensaje(),
                salon.getNombre()
        );

        // 5. Llamar al microservicio y propagar la respuesta
        log.info("Enviando notificación al microservicio: documento={}, salonId={}, salonNombre={}",
                request.documento(), salon.getId(), salon.getNombre());
        NotificacionMicroservicioResponse microResponse = notificacionClient.enviar(microRequest);

        return new NotificacionEnviadaResponse(microResponse.mensaje());
    }

    public void notificarAprobacionAlGestor(com.nelumbo.reservas.entity.Reserva reserva) {
        try {
            String mensaje = String.format(
                    "Tu reserva premium para el salón '%s' del cliente %s (doc. %s) ha sido APROBADA.",
                    reserva.getSalon().getNombre(),
                    reserva.getNombreCliente(),
                    reserva.getDocumentoCliente()
            );

            NotificacionMicroservicioRequest microRequest = new NotificacionMicroservicioRequest(
                    reserva.getGestor().getEmail(),
                    reserva.getDocumentoCliente(),
                    mensaje,
                    reserva.getSalon().getNombre()
            );

            log.info("Notificando al gestor {} la aprobación de la reserva id={}",
                    reserva.getGestor().getEmail(), reserva.getId());

            notificacionClient.enviar(microRequest);

        } catch (NotificacionMicroservicioException ex) {
            log.warn("Aprobación procesada, pero la notificación al gestor falló (reserva id={}): {}",
                    reserva.getId(), ex.getMessage());
        }
    }
}