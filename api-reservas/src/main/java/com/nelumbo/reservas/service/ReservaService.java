package com.nelumbo.reservas.service;

import com.nelumbo.reservas.dto.request.FinalizarReservaRequest;
import com.nelumbo.reservas.dto.request.RechazarReservaRequest;
import com.nelumbo.reservas.dto.request.ReservaRequest;
import com.nelumbo.reservas.dto.response.CrearReservaResponse;
import com.nelumbo.reservas.dto.response.FinalizarReservaResponse;
import com.nelumbo.reservas.dto.response.ReservaResponse;
import com.nelumbo.reservas.entity.Reserva;
import com.nelumbo.reservas.entity.ReservaHistorica;
import com.nelumbo.reservas.entity.Salon;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.EstadoReserva;
import com.nelumbo.reservas.exception.AccesoDenegadoException;
import com.nelumbo.reservas.exception.CapacidadSalonExcedidaException;
import com.nelumbo.reservas.exception.ReservaActivaExistenteException;
import com.nelumbo.reservas.exception.ReservaInvalidaException;
import com.nelumbo.reservas.exception.ReservaNoEncontradaException;
import com.nelumbo.reservas.exception.ReservaNoEsPendienteException;
import com.nelumbo.reservas.exception.SalonNoEncontradoException;
import com.nelumbo.reservas.mapper.ReservaMapper;
import com.nelumbo.reservas.repository.ReservaHistoricaRepository;
import com.nelumbo.reservas.repository.ReservaRepository;
import com.nelumbo.reservas.repository.SalonRepository;
import com.nelumbo.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final BigDecimal MONTO_PREMIUM = BigDecimal.valueOf(500_000);

    private static final List<EstadoReserva> ESTADOS_QUE_BLOQUEAN_DUPLICADO = List.of(
            EstadoReserva.ACTIVA,
            EstadoReserva.PENDIENTE_APROBACION
    );

    private final ReservaRepository reservaRepository;
    private final ReservaHistoricaRepository reservaHistoricaRepository;
    private final SalonRepository salonRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaMapper reservaMapper;
    private final NotificacionService notificacionService;

    @Transactional
    public CrearReservaResponse registrar(ReservaRequest request, String email, boolean esAdmin) {

        validarFechas(request);
        validarDocumentoSinReservaActiva(request.documentoCliente());

        Salon salon = buscarSalonOFallar(request.salonId());

        if (!esAdmin) {
            validarGestorEsDuenoDelSalon(email, salon);
        }

        validarCapacidadDisponible(
                salon,
                request.fechaInicio(),
                request.fechaFinEstimada(),
                request.asistentes()
        );

        BigDecimal costoEstimado = calcularCosto(
                salon.getCostoPorHora(),
                request.fechaInicio(),
                request.fechaFinEstimada()
        );

        Reserva reserva = new Reserva();
        reserva.setDocumentoCliente(request.documentoCliente());
        reserva.setNombreCliente(request.nombreCliente());
        reserva.setFechaInicio(request.fechaInicio());
        reserva.setFechaFinEstimada(request.fechaFinEstimada());
        reserva.setFechaCreacion(LocalDateTime.now());
        reserva.setAsistentes(request.asistentes());
        reserva.setSalon(salon);
        reserva.setGestor(salon.getGestor());

        if (costoEstimado.compareTo(MONTO_PREMIUM) > 0) {
            reserva.setEstado(EstadoReserva.PENDIENTE_APROBACION);
            log.info("Reserva premium creada (costo estimado={}). Queda PENDIENTE_APROBACION.", costoEstimado);
        } else {
            reserva.setEstado(EstadoReserva.ACTIVA);
        }

        Reserva guardada = reservaRepository.save(reserva);
        return new CrearReservaResponse(guardada.getId());
    }

    @Transactional
    public FinalizarReservaResponse finalizar(FinalizarReservaRequest request, String email, boolean esAdmin) {

        Salon salon = buscarSalonOFallar(request.salonId());

        if (!esAdmin) {
            validarGestorEsDuenoDelSalon(email, salon);
        }

        Reserva reserva = reservaRepository
                .findByDocumentoClienteAndSalonIdAndEstado(
                        request.documentoCliente(),
                        request.salonId(),
                        EstadoReserva.ACTIVA
                )
                .orElseThrow(ReservaNoEncontradaException::new);

        LocalDateTime fechaFinReal = LocalDateTime.now();

        BigDecimal totalCobrado = calcularCosto(
                salon.getCostoPorHora(),
                reserva.getFechaInicio(),
                fechaFinReal
        );

        ReservaHistorica historica = new ReservaHistorica();
        historica.setDocumentoCliente(reserva.getDocumentoCliente());
        historica.setNombreCliente(reserva.getNombreCliente());
        historica.setFechaInicio(reserva.getFechaInicio());
        historica.setFechaFinEstimada(reserva.getFechaFinEstimada());
        historica.setFechaCreacion(reserva.getFechaCreacion());
        historica.setFechaFinalizacionReal(fechaFinReal);
        historica.setAsistentes(reserva.getAsistentes());
        historica.setTotalCobrado(totalCobrado);
        historica.setSalon(reserva.getSalon());
        historica.setGestor(reserva.getGestor());

        reservaHistoricaRepository.save(historica);
        reservaRepository.delete(reserva);

        log.info("Reserva id={} finalizada. Total cobrado={}", reserva.getId(), totalCobrado);

        return new FinalizarReservaResponse("Reserva finalizada", totalCobrado);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarActivasPorSalon(Long salonId, String email, boolean esAdmin) {

        Salon salon = buscarSalonOFallar(salonId);

        if (!esAdmin) {
            validarGestorEsDuenoDelSalon(email, salon);
        }

        return reservaRepository
                .findBySalonIdAndEstado(salonId, EstadoReserva.ACTIVA)
                .stream()
                .map(reservaMapper::toResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ReservaResponse> buscarPorDocumentoParcial(String documento, String email, boolean esAdmin) {

        List<Reserva> reservas = reservaRepository
                .findByDocumentoClienteContainingAndEstado(documento, EstadoReserva.ACTIVA);

        if (esAdmin) {
            return reservas.stream().map(reservaMapper::toResponse).toList();
        }

        Usuario gestor = obtenerUsuarioPorEmail(email);
        return reservas.stream()
                .filter(r -> r.getGestor().getId().equals(gestor.getId()))
                .map(reservaMapper::toResponse)
                .toList();
    }


    @Transactional
    public ReservaResponse aprobar(Long reservaId) {

        Reserva reserva = buscarReservaOFallar(reservaId);

        if (reserva.getEstado() != EstadoReserva.PENDIENTE_APROBACION) {
            throw new ReservaNoEsPendienteException();
        }

        reserva.setEstado(EstadoReserva.ACTIVA);

        log.info("Reserva id={} APROBADA por ADMIN. Gestor responsable: {}",
                reserva.getId(), reserva.getGestor().getEmail());

        notificacionService.notificarAprobacionAlGestor(reserva);

        return reservaMapper.toResponse(reserva);
    }


    @Transactional
    public ReservaResponse rechazar(Long reservaId, RechazarReservaRequest request) {

        Reserva reserva = buscarReservaOFallar(reservaId);

        if (reserva.getEstado() != EstadoReserva.PENDIENTE_APROBACION) {
            throw new ReservaNoEsPendienteException();
        }

        reserva.setEstado(EstadoReserva.RECHAZADA);
        reserva.setMotivoRechazo(request.motivo());

        log.info("Reserva id={} RECHAZADA por ADMIN. Motivo: {}", reserva.getId(), request.motivo());

        return reservaMapper.toResponse(reserva);
    }



    private void validarFechas(ReservaRequest request) {
        if (!request.fechaFinEstimada().isAfter(request.fechaInicio())) {
            throw new ReservaInvalidaException();
        }
    }

    private void validarDocumentoSinReservaActiva(String documentoCliente) {
        boolean existe = reservaRepository.existsByDocumentoClienteAndEstadoIn(
                documentoCliente, ESTADOS_QUE_BLOQUEAN_DUPLICADO
        );
        if (existe) {
            throw new ReservaActivaExistenteException();
        }
    }

    private Salon buscarSalonOFallar(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new SalonNoEncontradoException(salonId));
    }

    private Reserva buscarReservaOFallar(Long reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));
    }

    private void validarGestorEsDuenoDelSalon(String email, Salon salon) {
        Usuario gestor = obtenerUsuarioPorEmail(email);
        if (!salon.getGestor().getId().equals(gestor.getId())) {
            throw new AccesoDenegadoException("No tienes acceso a este salón");
        }
    }

    private void validarCapacidadDisponible(Salon salon,
                                            LocalDateTime fechaInicio,
                                            LocalDateTime fechaFin,
                                            Integer asistentesNuevos) {

        long asistentesActuales = reservaRepository
                .sumarAsistentesReservasSolapadas(salon.getId(), fechaInicio, fechaFin);

        if ((asistentesActuales + asistentesNuevos) > salon.getCapacidad()) {
            throw new CapacidadSalonExcedidaException();
        }
    }

    private Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    private BigDecimal calcularCosto(BigDecimal costoPorHora,
                                     LocalDateTime fechaInicio,
                                     LocalDateTime fechaFin) {

        long minutos = Duration.between(fechaInicio, fechaFin).toMinutes();
        long horasCobradas = (long) Math.ceil(minutos / 60.0);
        if (horasCobradas < 1) {
            horasCobradas = 1;
        }
        return costoPorHora.multiply(BigDecimal.valueOf(horasCobradas));
    }
}
