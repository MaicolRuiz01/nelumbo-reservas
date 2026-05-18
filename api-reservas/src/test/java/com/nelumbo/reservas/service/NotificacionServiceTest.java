package com.nelumbo.reservas.service;

import com.nelumbo.reservas.client.NotificacionClient;
import com.nelumbo.reservas.client.dto.NotificacionMicroservicioRequest;
import com.nelumbo.reservas.client.dto.NotificacionMicroservicioResponse;
import com.nelumbo.reservas.dto.request.NotificacionEnviarRequest;
import com.nelumbo.reservas.dto.response.NotificacionEnviadaResponse;
import com.nelumbo.reservas.entity.Reserva;
import com.nelumbo.reservas.entity.Salon;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.EstadoReserva;
import com.nelumbo.reservas.enums.Rol;
import com.nelumbo.reservas.exception.NotificacionReservaActivaInexistenteException;
import com.nelumbo.reservas.repository.ReservaRepository;
import com.nelumbo.reservas.repository.SalonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private SalonRepository salonRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private NotificacionClient notificacionClient;

    @InjectMocks private NotificacionService notificacionService;

    private Usuario gestor;
    private Salon salon;

    @BeforeEach
    void setUp() {
        gestor = new Usuario();
        gestor.setId(10L);
        gestor.setEmail("gestor@mail.com");
        gestor.setNombre("Gestor Test");
        gestor.setRol(Rol.GESTOR);

        salon = new Salon();
        salon.setId(1L);
        salon.setNombre("Salón Cristal");
        salon.setCapacidad(50);
        salon.setCostoPorHora(BigDecimal.valueOf(100_000));
        salon.setGestor(gestor);
    }

    @Test
    @DisplayName("enviar: si no existe reserva activa para el documento+salón, lanza excepción y NO llama al microservicio")
    void enviar_sinReservaActiva_lanzaExcepcionYNoLlamaAlMicro() {
        // Arrange
        NotificacionEnviarRequest request = new NotificacionEnviarRequest(
                "cliente@mail.com", "1234567890", "Recordatorio de tu evento", 1L);

        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        // No hay reserva activa para ese documento en ese salón
        when(reservaRepository.findByDocumentoClienteAndSalonIdAndEstado(
                "1234567890", 1L, EstadoReserva.ACTIVA))
                .thenReturn(Optional.empty());

        // Act + Assert — usuario admin (esAdmin=true) para saltar la validación de ownership
        assertThatThrownBy(() ->
                notificacionService.enviar(request, "admin@mail.com", true))
                .isInstanceOf(NotificacionReservaActivaInexistenteException.class)
                .hasMessage("No se puede Enviar Notificación, no existe una reserva activa para este documento en el salón indicado");

        // El microservicio JAMÁS debe ser invocado
        verify(notificacionClient, never()).enviar(any());
    }

    @Test
    @DisplayName("enviar: con reserva activa, transforma salonId → salonNombre y propaga la respuesta del microservicio")
    void enviar_conReservaActiva_transformaSalonIdEnNombreYDelegaAlMicro() {
        // Arrange
        NotificacionEnviarRequest request = new NotificacionEnviarRequest(
                "cliente@mail.com", "1234567890", "Recordatorio de tu evento", 1L);

        Reserva reservaActiva = new Reserva();
        reservaActiva.setId(99L);
        reservaActiva.setDocumentoCliente("1234567890");
        reservaActiva.setSalon(salon);
        reservaActiva.setEstado(EstadoReserva.ACTIVA);

        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(reservaRepository.findByDocumentoClienteAndSalonIdAndEstado(
                "1234567890", 1L, EstadoReserva.ACTIVA))
                .thenReturn(Optional.of(reservaActiva));
        when(notificacionClient.enviar(any(NotificacionMicroservicioRequest.class)))
                .thenReturn(new NotificacionMicroservicioResponse("Notificación Enviada"));

        // Act
        NotificacionEnviadaResponse response =
                notificacionService.enviar(request, "admin@mail.com", true);

        // Assert — la respuesta del micro se propaga tal cual
        assertThat(response.mensaje()).isEqualTo("Notificación Enviada");

        // El request al micro lleva salonNombre, NO salonId
        ArgumentCaptor<NotificacionMicroservicioRequest> captor =
                ArgumentCaptor.forClass(NotificacionMicroservicioRequest.class);
        verify(notificacionClient).enviar(captor.capture());
        NotificacionMicroservicioRequest enviado = captor.getValue();

        assertThat(enviado.email()).isEqualTo("cliente@mail.com");
        assertThat(enviado.documento()).isEqualTo("1234567890");
        assertThat(enviado.mensaje()).isEqualTo("Recordatorio de tu evento");
        assertThat(enviado.salonNombre())
                .as("El PDF exige transformar salonId → salonNombre antes de llamar al micro")
                .isEqualTo("Salón Cristal");
    }
}
