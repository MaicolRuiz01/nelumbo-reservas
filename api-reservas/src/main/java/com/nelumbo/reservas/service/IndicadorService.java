package com.nelumbo.reservas.service;

import com.nelumbo.reservas.dto.response.GananciasResponse;
import com.nelumbo.reservas.dto.response.ReservaResponse;
import com.nelumbo.reservas.dto.response.TopClienteResponse;
import com.nelumbo.reservas.dto.response.TopSucursalResponse;
import com.nelumbo.reservas.entity.Reserva;
import com.nelumbo.reservas.entity.Salon;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.exception.AccesoDenegadoException;
import com.nelumbo.reservas.exception.SalonNoEncontradoException;
import com.nelumbo.reservas.mapper.ReservaMapper;
import com.nelumbo.reservas.repository.ReservaHistoricaRepository;
import com.nelumbo.reservas.repository.ReservaRepository;
import com.nelumbo.reservas.repository.SalonRepository;
import com.nelumbo.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicadorService {

    private final ReservaRepository reservaRepository;
    private final ReservaHistoricaRepository reservaHistoricaRepository;
    private final SalonRepository salonRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaMapper reservaMapper;

    // === Helpers privados que vamos a usar en varios bloques ===

    private Salon buscarSalonOFallar(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new SalonNoEncontradoException(salonId));
    }

    private void validarGestorEsDuenoDelSalon(String email, Salon salon) {
        Usuario gestor = obtenerUsuarioPorEmail(email);
        if (!salon.getGestor().getId().equals(gestor.getId())) {
            throw new AccesoDenegadoException("No tienes acceso a este salón");
        }
    }

    private Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    @Transactional(readOnly = true)
    public List<TopClienteResponse> topClientesGlobal() {

        List<TopClienteResponse> historicas =
                reservaHistoricaRepository.contarReservasPorClienteGlobal();

        List<TopClienteResponse> activas =
                reservaRepository.contarReservasActivasPorClienteGlobal();

        Map<String, TopClienteResponse> combinado = new HashMap<>();

        Stream.concat(historicas.stream(), activas.stream()).forEach(tc ->
                combinado.merge(
                        tc.documentoCliente(),
                        tc,
                        (existente, nuevo) -> new TopClienteResponse(
                                existente.documentoCliente(),
                                existente.nombreCliente(),
                                existente.cantidad() + nuevo.cantidad()
                        )
                )
        );

        return combinado.values().stream()
                .sorted(Comparator.comparingLong(TopClienteResponse::cantidad).reversed())
                .limit(10)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopClienteResponse> topClientesPorSalon(
            Long salonId,
            String email,
            boolean esAdmin
    ) {

        Salon salon = buscarSalonOFallar(salonId);

        if (!esAdmin) {
            validarGestorEsDuenoDelSalon(email, salon);
        }

        List<TopClienteResponse> historicas =
                reservaHistoricaRepository.contarReservasPorClienteEnSalon(salonId);

        List<TopClienteResponse> activas =
                reservaRepository.contarReservasActivasPorClienteEnSalon(salonId);

        Map<String, TopClienteResponse> combinado = new HashMap<>();

        Stream.concat(historicas.stream(), activas.stream()).forEach(tc ->
                combinado.merge(
                        tc.documentoCliente(),
                        tc,
                        (existente, nuevo) -> new TopClienteResponse(
                                existente.documentoCliente(),
                                existente.nombreCliente(),
                                existente.cantidad() + nuevo.cantidad()
                        )
                )
        );

        return combinado.values().stream()
                .sorted(Comparator.comparingLong(TopClienteResponse::cantidad).reversed())
                .limit(10)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> clientesPrimeraVezPorSalon(
            Long salonId,
            String email,
            boolean esAdmin
    ) {

        Salon salon = buscarSalonOFallar(salonId);

        if (!esAdmin) {
            validarGestorEsDuenoDelSalon(email, salon);
        }

        List<Reserva> reservas =
                reservaRepository.buscarReservasActivasPrimeraVez(salonId);

        return reservas.stream()
                .map(reservaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GananciasResponse gananciasPorSalon(
            Long salonId,
            String email,
            boolean esAdmin
    ) {

        Salon salon = buscarSalonOFallar(salonId);

        if (!esAdmin) {
            validarGestorEsDuenoDelSalon(email, salon);
        }

        LocalDate hoy = LocalDate.now();

        // === HOY ===
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = hoy.plusDays(1).atStartOfDay();

        // === SEMANA ===
        LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
        LocalDate inicioProximaSemana = inicioSemana.plusWeeks(1);

        // === MES ===
        LocalDate inicioMes = hoy.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate inicioProximoMes = inicioMes.plusMonths(1);

        // === AÑO ===
        LocalDate inicioAnio = hoy.with(TemporalAdjusters.firstDayOfYear());
        LocalDate inicioProximoAnio = inicioAnio.plusYears(1);

        BigDecimal gananciasHoy =
                reservaHistoricaRepository.sumarTotalCobradoPorRango(
                        salonId,
                        inicioHoy,
                        inicioManana
                );

        BigDecimal gananciasSemana =
                reservaHistoricaRepository.sumarTotalCobradoPorRango(
                        salonId,
                        inicioSemana.atStartOfDay(),
                        inicioProximaSemana.atStartOfDay()
                );

        BigDecimal gananciasMes =
                reservaHistoricaRepository.sumarTotalCobradoPorRango(
                        salonId,
                        inicioMes.atStartOfDay(),
                        inicioProximoMes.atStartOfDay()
                );

        BigDecimal gananciasAnio =
                reservaHistoricaRepository.sumarTotalCobradoPorRango(
                        salonId,
                        inicioAnio.atStartOfDay(),
                        inicioProximoAnio.atStartOfDay()
                );

        return new GananciasResponse(
                gananciasHoy,
                gananciasSemana,
                gananciasMes,
                gananciasAnio
        );
    }

    @Transactional(readOnly = true)
    public List<TopSucursalResponse> topSucursalesMesActual() {

        LocalDate hoy = LocalDate.now();

        LocalDate inicioMes =
                hoy.with(TemporalAdjusters.firstDayOfMonth());

        LocalDate inicioProximoMes =
                inicioMes.plusMonths(1);

        return reservaHistoricaRepository.topSucursalesPorFacturacion(
                inicioMes.atStartOfDay(),
                inicioProximoMes.atStartOfDay(),
                PageRequest.of(0, 3)
        );
    }
}