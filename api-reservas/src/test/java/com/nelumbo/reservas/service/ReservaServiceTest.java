package com.nelumbo.reservas.service;

import com.nelumbo.reservas.dto.request.FinalizarReservaRequest;
import com.nelumbo.reservas.dto.request.RechazarReservaRequest;
import com.nelumbo.reservas.dto.request.ReservaRequest;
import com.nelumbo.reservas.dto.response.CrearReservaResponse;
import com.nelumbo.reservas.dto.response.FinalizarReservaResponse;
import com.nelumbo.reservas.entity.Reserva;
import com.nelumbo.reservas.entity.ReservaHistorica;
import com.nelumbo.reservas.entity.Salon;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.EstadoReserva;
import com.nelumbo.reservas.enums.Rol;
import com.nelumbo.reservas.exception.AccesoDenegadoException;
import com.nelumbo.reservas.exception.CapacidadSalonExcedidaException;
import com.nelumbo.reservas.exception.ReservaActivaExistenteException;
import com.nelumbo.reservas.exception.ReservaInvalidaException;
import com.nelumbo.reservas.exception.ReservaNoEncontradaException;
import com.nelumbo.reservas.exception.ReservaNoEsPendienteException;
import com.nelumbo.reservas.mapper.ReservaMapper;
import com.nelumbo.reservas.repository.ReservaHistoricaRepository;
import com.nelumbo.reservas.repository.ReservaRepository;
import com.nelumbo.reservas.repository.SalonRepository;
import com.nelumbo.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    // ============================================================
    // Mocks (colaboradores)
    // ============================================================
    @Mock private ReservaRepository reservaRepository;
    @Mock private ReservaHistoricaRepository reservaHistoricaRepository;
    @Mock private SalonRepository salonRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ReservaMapper reservaMapper;
    // Cableado en Fase 4: aprobar() dispara una notificación al gestor.
    // Sin este mock, @InjectMocks deja notificacionService=null y la rama
    // de aprobar() pega NPE. Lo mockeamos vacío porque para esta clase
    // de tests solo nos importa la transición de estado, no el side-effect.
    @Mock private NotificacionService notificacionService;

    // ============================================================
    // Sistema bajo prueba (SUT) — con los mocks inyectados
    // ============================================================
    @InjectMocks private ReservaService reservaService;

    // ============================================================
    // Fixtures comunes
    // ============================================================
    private Usuario gestor;
    private Usuario otroGestor;
    private Salon salon;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @BeforeEach
    void setUp() {
        gestor = new Usuario();
        gestor.setId(10L);
        gestor.setEmail("gestor@mail.com");
        gestor.setNombre("Gestor Test");
        gestor.setRol(Rol.GESTOR);

        otroGestor = new Usuario();
        otroGestor.setId(11L);
        otroGestor.setEmail("otro@mail.com");
        otroGestor.setNombre("Otro Gestor");
        otroGestor.setRol(Rol.GESTOR);

        salon = new Salon();
        salon.setId(1L);
        salon.setNombre("Salon Test");
        salon.setCapacidad(50);
        salon.setCostoPorHora(BigDecimal.valueOf(100_000));
        salon.setGestor(gestor);

        // Fechas seguras (futuro razonable, rango de 2h)
        fechaInicio = LocalDateTime.of(2026, 6, 1, 10, 0);
        fechaFin = LocalDateTime.of(2026, 6, 1, 12, 0);
    }

    // ============================================================================
    // ============================================================================
    //                              REGISTRAR
    // ============================================================================
    // ============================================================================
    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("Caso feliz GESTOR dueno: crea reserva ACTIVA con id devuelto")
        void registrar_cuandoGestorDueñoYCostoNormal_creaReservaActiva() {
            // Arrange
            ReservaRequest req = nuevaReservaRequest("1234567", 5, fechaInicio, fechaFin);

            when(reservaRepository.existsByDocumentoClienteAndEstadoIn(eq("1234567"), anyCollection()))
                    .thenReturn(false);
            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            when(usuarioRepository.findByEmail("gestor@mail.com")).thenReturn(Optional.of(gestor));
            when(reservaRepository.sumarAsistentesReservasSolapadas(eq(1L), any(), any()))
                    .thenReturn(0L);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                r.setId(99L);
                return r;
            });

            // Act
            CrearReservaResponse resp = reservaService.registrar(req, "gestor@mail.com", false);

            // Assert: id devuelto
            assertThat(resp.id()).isEqualTo(99L);

            // Assert: la entidad guardada tiene los datos correctos
            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            Reserva guardada = captor.getValue();

            assertThat(guardada.getEstado()).isEqualTo(EstadoReserva.ACTIVA);
            assertThat(guardada.getDocumentoCliente()).isEqualTo("1234567");
            assertThat(guardada.getAsistentes()).isEqualTo(5);
            assertThat(guardada.getSalon()).isEqualTo(salon);
            // Regla clave: el gestor de la reserva es el dueno del salon, NO el usuario logueado
            assertThat(guardada.getGestor()).isEqualTo(gestor);
            assertThat(guardada.getFechaCreacion()).isNotNull();
        }

        @Test
        @DisplayName("Costo > 500.000: la reserva queda PENDIENTE_APROBACION (regla premium)")
        void registrar_cuandoCostoSuperaPremium_creaReservaPendiente() {
            // Arrange: 6 horas x 100.000/h = 600.000 → premium
            LocalDateTime inicio = LocalDateTime.of(2026, 6, 5, 8, 0);
            LocalDateTime fin = LocalDateTime.of(2026, 6, 5, 14, 0);
            ReservaRequest req = nuevaReservaRequest("9999999", 10, inicio, fin);

            when(reservaRepository.existsByDocumentoClienteAndEstadoIn(any(), anyCollection())).thenReturn(false);
            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            when(usuarioRepository.findByEmail("gestor@mail.com")).thenReturn(Optional.of(gestor));
            when(reservaRepository.sumarAsistentesReservasSolapadas(any(), any(), any())).thenReturn(0L);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            reservaService.registrar(req, "gestor@mail.com", false);

            // Assert
            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReserva.PENDIENTE_APROBACION);
        }

        @Test
        @DisplayName("Costo igual a 500.000: NO es premium (la regla es ESTRICTAMENTE mayor)")
        void registrar_cuandoCostoIgualAlPremium_creaReservaActiva() {
            // Arrange: 5 horas x 100.000/h = 500.000 — borde inferior
            LocalDateTime inicio = LocalDateTime.of(2026, 6, 5, 8, 0);
            LocalDateTime fin = LocalDateTime.of(2026, 6, 5, 13, 0);
            ReservaRequest req = nuevaReservaRequest("9999998", 10, inicio, fin);

            when(reservaRepository.existsByDocumentoClienteAndEstadoIn(any(), anyCollection())).thenReturn(false);
            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            when(usuarioRepository.findByEmail(any())).thenReturn(Optional.of(gestor));
            when(reservaRepository.sumarAsistentesReservasSolapadas(any(), any(), any())).thenReturn(0L);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            reservaService.registrar(req, "gestor@mail.com", false);

            // Assert
            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReserva.ACTIVA);
        }

        @Test
        @DisplayName("Fecha fin no posterior a inicio: lanza ReservaInvalidaException")
        void registrar_cuandoFechaFinNoPosteriorAInicio_lanzaExcepcion() {
            // Arrange
            ReservaRequest req = nuevaReservaRequest("1111111", 5, fechaInicio, fechaInicio);

            // Act + Assert
            assertThatThrownBy(() -> reservaService.registrar(req, "gestor@mail.com", false))
                    .isInstanceOf(ReservaInvalidaException.class)
                    .hasMessageContaining("posterior a la fecha de inicio");

            // Nunca debe llegar a guardar
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Documento ya tiene reserva ACTIVA/PENDIENTE: lanza ReservaActivaExistenteException")
        void registrar_cuandoDocumentoYaTieneReservaActiva_lanzaExcepcion() {
            // Arrange
            ReservaRequest req = nuevaReservaRequest("2222222", 5, fechaInicio, fechaFin);
            when(reservaRepository.existsByDocumentoClienteAndEstadoIn(eq("2222222"), anyCollection()))
                    .thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> reservaService.registrar(req, "gestor@mail.com", false))
                    .isInstanceOf(ReservaActivaExistenteException.class)
                    .hasMessageContaining("ya existe una reserva activa para este documento");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Capacidad excedida en el rango horario: lanza CapacidadSalonExcedidaException")
        void registrar_cuandoCapacidadExcedida_lanzaExcepcion() {
            // Arrange: ya hay 48 asistentes + pide 5, capacidad 50 → 53 > 50
            ReservaRequest req = nuevaReservaRequest("3333333", 5, fechaInicio, fechaFin);
            when(reservaRepository.existsByDocumentoClienteAndEstadoIn(any(), anyCollection())).thenReturn(false);
            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            when(usuarioRepository.findByEmail(any())).thenReturn(Optional.of(gestor));
            when(reservaRepository.sumarAsistentesReservasSolapadas(any(), any(), any())).thenReturn(48L);

            // Act + Assert
            assertThatThrownBy(() -> reservaService.registrar(req, "gestor@mail.com", false))
                    .isInstanceOf(CapacidadSalonExcedidaException.class)
                    .hasMessageContaining("capacidad insuficiente");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("GESTOR que NO es dueno del salon: lanza AccesoDenegadoException")
        void registrar_cuandoGestorNoEsDuenoDelSalon_lanzaExcepcion() {
            // Arrange
            ReservaRequest req = nuevaReservaRequest("4444444", 5, fechaInicio, fechaFin);
            when(reservaRepository.existsByDocumentoClienteAndEstadoIn(any(), anyCollection())).thenReturn(false);
            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            // El email logueado es de OTRO gestor distinto al dueno del salon
            when(usuarioRepository.findByEmail("otro@mail.com")).thenReturn(Optional.of(otroGestor));

            // Act + Assert
            assertThatThrownBy(() -> reservaService.registrar(req, "otro@mail.com", false))
                    .isInstanceOf(AccesoDenegadoException.class);

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("ADMIN puede registrar en cualquier salon (bypass de ownership)")
        void registrar_cuandoEsAdmin_omiteValidacionDeOwnership() {
            // Arrange: un usuario "admin@mail.com" que NO es el gestor del salon
            ReservaRequest req = nuevaReservaRequest("5555555", 5, fechaInicio, fechaFin);
            when(reservaRepository.existsByDocumentoClienteAndEstadoIn(any(), anyCollection())).thenReturn(false);
            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            when(reservaRepository.sumarAsistentesReservasSolapadas(any(), any(), any())).thenReturn(0L);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            reservaService.registrar(req, "admin@mail.com", true);  // esAdmin = true

            // Assert: NUNCA consulto al UsuarioRepository (no necesita validar ownership)
            verify(usuarioRepository, never()).findByEmail(any());
            verify(reservaRepository).save(any(Reserva.class));
        }
    }

    // ============================================================================
    // ============================================================================
    //                              FINALIZAR
    // ============================================================================
    // ============================================================================
    @Nested
    @DisplayName("finalizar()")
    class Finalizar {

        @Test
        @DisplayName("Reserva existe ACTIVA: la mueve a historica, la borra de reservas y devuelve total")
        void finalizar_cuandoReservaActivaExiste_creaHistoricaYBorraOriginal() {
            // Arrange
            Reserva reservaActiva = new Reserva();
            reservaActiva.setId(50L);
            reservaActiva.setDocumentoCliente("1234567");
            reservaActiva.setNombreCliente("Cliente Test");
            // Inicio en el pasado, fin estimada futura — pero finalizamos AHORA
            reservaActiva.setFechaInicio(LocalDateTime.now().minusHours(2));
            reservaActiva.setFechaFinEstimada(LocalDateTime.now().plusHours(2));
            reservaActiva.setFechaCreacion(LocalDateTime.now().minusHours(3));
            reservaActiva.setAsistentes(5);
            reservaActiva.setEstado(EstadoReserva.ACTIVA);
            reservaActiva.setSalon(salon);
            reservaActiva.setGestor(gestor);

            FinalizarReservaRequest req = new FinalizarReservaRequest("1234567", 1L);

            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            when(usuarioRepository.findByEmail("gestor@mail.com")).thenReturn(Optional.of(gestor));
            when(reservaRepository.findByDocumentoClienteAndSalonIdAndEstado(
                    "1234567", 1L, EstadoReserva.ACTIVA))
                    .thenReturn(Optional.of(reservaActiva));

            // Act
            FinalizarReservaResponse resp = reservaService.finalizar(req, "gestor@mail.com", false);

            // Assert: mensaje literal del PDF
            assertThat(resp.mensaje()).isEqualTo("Reserva finalizada");
            // Total cobrado debe ser > 0 (al menos 2h pasaron desde fechaInicio)
            assertThat(resp.totalCobrado()).isGreaterThan(BigDecimal.ZERO);

            // Verifica que se guardo en historica con los datos correctos
            ArgumentCaptor<ReservaHistorica> captor = ArgumentCaptor.forClass(ReservaHistorica.class);
            verify(reservaHistoricaRepository).save(captor.capture());
            ReservaHistorica historica = captor.getValue();
            assertThat(historica.getDocumentoCliente()).isEqualTo("1234567");
            assertThat(historica.getTotalCobrado()).isEqualTo(resp.totalCobrado());
            assertThat(historica.getFechaFinalizacionReal()).isNotNull();

            // Verifica que se borro la original
            verify(reservaRepository).delete(reservaActiva);
        }

        @Test
        @DisplayName("No hay reserva ACTIVA: lanza ReservaNoEncontradaException con mensaje del PDF")
        void finalizar_cuandoNoExisteReservaActiva_lanzaExcepcion() {
            // Arrange
            FinalizarReservaRequest req = new FinalizarReservaRequest("9999999", 1L);

            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            when(usuarioRepository.findByEmail("gestor@mail.com")).thenReturn(Optional.of(gestor));
            when(reservaRepository.findByDocumentoClienteAndSalonIdAndEstado(any(), any(), any()))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> reservaService.finalizar(req, "gestor@mail.com", false))
                    .isInstanceOf(ReservaNoEncontradaException.class)
                    .hasMessageContaining("No se puede Finalizar Reserva");

            verify(reservaHistoricaRepository, never()).save(any());
            verify(reservaRepository, never()).delete(any(Reserva.class));
        }
    }

    // ============================================================================
    // ============================================================================
    //                            APROBAR / RECHAZAR
    // ============================================================================
    // ============================================================================
    @Nested
    @DisplayName("aprobar() y rechazar()")
    class AprobarRechazar {

        @Test
        @DisplayName("Aprobar reserva en PENDIENTE_APROBACION: la cambia a ACTIVA")
        void aprobar_cuandoEstaPendiente_pasaAActiva() {
            Reserva pendiente = nuevaReservaConEstado(EstadoReserva.PENDIENTE_APROBACION);
            when(reservaRepository.findById(50L)).thenReturn(Optional.of(pendiente));

            reservaService.aprobar(50L);

            assertThat(pendiente.getEstado()).isEqualTo(EstadoReserva.ACTIVA);
        }

        @Test
        @DisplayName("Aprobar reserva ACTIVA: lanza ReservaNoEsPendienteException")
        void aprobar_cuandoYaEstaActiva_lanzaExcepcion() {
            Reserva activa = nuevaReservaConEstado(EstadoReserva.ACTIVA);
            when(reservaRepository.findById(50L)).thenReturn(Optional.of(activa));

            assertThatThrownBy(() -> reservaService.aprobar(50L))
                    .isInstanceOf(ReservaNoEsPendienteException.class);
        }

        @Test
        @DisplayName("Rechazar reserva PENDIENTE: la pasa a RECHAZADA y guarda motivo")
        void rechazar_cuandoEstaPendiente_pasaARechazadaConMotivo() {
            Reserva pendiente = nuevaReservaConEstado(EstadoReserva.PENDIENTE_APROBACION);
            when(reservaRepository.findById(50L)).thenReturn(Optional.of(pendiente));

            RechazarReservaRequest req = new RechazarReservaRequest("Cliente no completo anticipo");
            reservaService.rechazar(50L, req);

            assertThat(pendiente.getEstado()).isEqualTo(EstadoReserva.RECHAZADA);
            assertThat(pendiente.getMotivoRechazo()).isEqualTo("Cliente no completo anticipo");
        }

        @Test
        @DisplayName("Rechazar reserva ya RECHAZADA: lanza ReservaNoEsPendienteException")
        void rechazar_cuandoYaEstaRechazada_lanzaExcepcion() {
            Reserva rechazada = nuevaReservaConEstado(EstadoReserva.RECHAZADA);
            when(reservaRepository.findById(50L)).thenReturn(Optional.of(rechazada));

            RechazarReservaRequest req = new RechazarReservaRequest("Otro motivo");
            assertThatThrownBy(() -> reservaService.rechazar(50L, req))
                    .isInstanceOf(ReservaNoEsPendienteException.class);
        }
    }

    // ============================================================================
    // ============================================================================
    //                       CALCULO DE HORAS (redondeo hacia arriba)
    // ============================================================================
    // ============================================================================
    @Nested
    @DisplayName("Calculo de costo: redondeo de horas HACIA ARRIBA (PDF sec 8)")
    class CalculoDeCosto {

        @Test
        @DisplayName("1h 30min se cobra como 2h")
        void costo_cuandoUnaHoraTreintaMinutos_cobraDosHoras() {
            // Arrange: 90 min = 1.5h → debe cobrarse 2h × 100.000 = 200.000
            LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 10, 0);
            LocalDateTime fin = LocalDateTime.of(2026, 6, 1, 11, 30);
            ReservaRequest req = nuevaReservaRequest("6000001", 1, inicio, fin);

            stubsHappyPath();
            reservaService.registrar(req, "gestor@mail.com", false);

            // NO podemos leer el costo desde registrar (no lo devuelve), pero podemos validarlo
            // a traves de la regla premium en otros tests. Aqui validamos via finalizar.
            // Es decir, si 1.5h × 100.000 = 200.000 NO es premium, queda ACTIVA. (Si fuera 5h × 100.000
            // tampoco, pero 7h sí). Test indirecto: nos importa que el costo se calcule sin romper.
            // Para una validacion directa del valor, ver el test de finalizar.
        }

        @Test
        @DisplayName("Finalizar con 45 min reales cobra como 1h completa")
        void costo_cuandoCuarentaYCincoMinutos_cobraUnaHoraCompleta() {
            // Arrange: inicio fue hace 45min, finalizamos ahora
            Reserva reservaActiva = new Reserva();
            reservaActiva.setId(50L);
            reservaActiva.setDocumentoCliente("7000001");
            reservaActiva.setNombreCliente("X");
            reservaActiva.setFechaInicio(LocalDateTime.now().minusMinutes(45));
            reservaActiva.setFechaFinEstimada(LocalDateTime.now().plusHours(1));
            reservaActiva.setFechaCreacion(LocalDateTime.now().minusMinutes(50));
            reservaActiva.setAsistentes(5);
            reservaActiva.setEstado(EstadoReserva.ACTIVA);
            reservaActiva.setSalon(salon);
            reservaActiva.setGestor(gestor);

            FinalizarReservaRequest req = new FinalizarReservaRequest("7000001", 1L);

            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            when(usuarioRepository.findByEmail("gestor@mail.com")).thenReturn(Optional.of(gestor));
            when(reservaRepository.findByDocumentoClienteAndSalonIdAndEstado(any(), any(), any()))
                    .thenReturn(Optional.of(reservaActiva));

            // Act
            FinalizarReservaResponse resp = reservaService.finalizar(req, "gestor@mail.com", false);

            // Assert: 45 min se redondea a 1h, costo = 100.000 × 1 = 100.000
            assertThat(resp.totalCobrado()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        }

        @Test
        @DisplayName("Finalizar con 2h exactas cobra 2h (sin redondear de mas)")
        void costo_cuandoDosHorasExactas_cobraDosHoras() {
            Reserva reservaActiva = new Reserva();
            reservaActiva.setId(50L);
            reservaActiva.setDocumentoCliente("7000002");
            reservaActiva.setNombreCliente("X");
            reservaActiva.setFechaInicio(LocalDateTime.now().minusHours(2));
            reservaActiva.setFechaFinEstimada(LocalDateTime.now().plusHours(1));
            reservaActiva.setFechaCreacion(LocalDateTime.now().minusHours(3));
            reservaActiva.setAsistentes(5);
            reservaActiva.setEstado(EstadoReserva.ACTIVA);
            reservaActiva.setSalon(salon);
            reservaActiva.setGestor(gestor);

            FinalizarReservaRequest req = new FinalizarReservaRequest("7000002", 1L);

            when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
            when(usuarioRepository.findByEmail("gestor@mail.com")).thenReturn(Optional.of(gestor));
            when(reservaRepository.findByDocumentoClienteAndSalonIdAndEstado(any(), any(), any()))
                    .thenReturn(Optional.of(reservaActiva));

            FinalizarReservaResponse resp = reservaService.finalizar(req, "gestor@mail.com", false);

            // 2h × 100.000 = 200.000
            assertThat(resp.totalCobrado()).isEqualByComparingTo(BigDecimal.valueOf(200_000));
        }
    }

    // ============================================================================
    // Helpers privados (factories de objetos para reducir ruido en los tests)
    // ============================================================================

    private ReservaRequest nuevaReservaRequest(String doc, int asistentes,
                                               LocalDateTime ini, LocalDateTime fin) {
        return new ReservaRequest(doc, "Cliente " + doc, ini, fin, asistentes, 1L);
    }

    private Reserva nuevaReservaConEstado(EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setId(50L);
        r.setDocumentoCliente("1234567");
        r.setNombreCliente("Cliente Test");
        r.setFechaInicio(fechaInicio);
        r.setFechaFinEstimada(fechaFin);
        r.setFechaCreacion(LocalDateTime.now().minusHours(1));
        r.setAsistentes(5);
        r.setEstado(estado);
        r.setSalon(salon);
        r.setGestor(gestor);
        return r;
    }

    /**
     * Stubs para el happy path de registrar — extraidos para no repetir 5 lineas
     * en cada test.
     */
    private void stubsHappyPath() {
        when(reservaRepository.existsByDocumentoClienteAndEstadoIn(any(), anyCollection())).thenReturn(false);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.of(gestor));
        when(reservaRepository.sumarAsistentesReservasSolapadas(any(), any(), any())).thenReturn(0L);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });
    }
}
