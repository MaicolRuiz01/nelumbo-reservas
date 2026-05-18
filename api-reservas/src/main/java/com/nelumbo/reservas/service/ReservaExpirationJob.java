package com.nelumbo.reservas.service;

import com.nelumbo.reservas.entity.Reserva;
import com.nelumbo.reservas.enums.EstadoReserva;
import com.nelumbo.reservas.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaExpirationJob {

    private static final long FRECUENCIA_MS = 15 * 60 * 1000L; // 15 minutos
    private static final long HORAS_PARA_EXPIRAR = 48;

    private final ReservaRepository reservaRepository;

    @Scheduled(fixedDelay = FRECUENCIA_MS, initialDelay = 60_000L)
    @Transactional
    public void expirarReservasPendientes() {

        LocalDateTime limite = LocalDateTime.now().minusHours(HORAS_PARA_EXPIRAR);

        List<Reserva> aExpirar = reservaRepository
                .findByEstadoAndFechaCreacionBefore(EstadoReserva.PENDIENTE_APROBACION, limite);

        if (aExpirar.isEmpty()) {
            log.debug("Job de expiración: no hay reservas premium para expirar.");
            return;
        }

        aExpirar.forEach(r -> r.setEstado(EstadoReserva.EXPIRADA));

        log.info("Job de expiración: {} reserva(s) premium marcada(s) como EXPIRADA (>48h en PENDIENTE_APROBACION).",
                aExpirar.size());
    }
}
