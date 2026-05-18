package com.nelumbo.reservas.repository;

import com.nelumbo.reservas.entity.Reserva;
import com.nelumbo.reservas.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByDocumentoClienteAndEstadoIn(
            String documentoCliente,
            Collection<EstadoReserva> estados
    );

    List<Reserva> findBySalonIdAndEstado(
            Long salonId,
            EstadoReserva estado
    );

    List<Reserva> findByDocumentoClienteContainingAndEstado(
            String documento,
            EstadoReserva estado
    );

    Optional<Reserva> findByDocumentoClienteAndSalonIdAndEstado(
            String documentoCliente,
            Long salonId,
            EstadoReserva estado
    );

    List<Reserva> findByEstadoAndFechaCreacionBefore(
            EstadoReserva estado,
            LocalDateTime fechaLimite
    );

    /**
     * Suma de asistentes de reservas ACTIVAS cuyo rango horario se solapa con el
     * intervalo [:fechaInicio, :fechaFin]. Las PENDIENTE_APROBACION no se cuentan
     * porque por regla del PDF no ocupan cupo hasta ser aprobadas.
     *
     * Devuelve Long porque Hibernate 6 promueve SUM(integer) a Long.
     */
    @Query("""
        SELECT COALESCE(SUM(r.asistentes), 0)
        FROM Reserva r
        WHERE r.salon.id = :salonId
          AND r.estado = com.nelumbo.reservas.enums.EstadoReserva.ACTIVA
          AND r.fechaInicio < :fechaFin
          AND r.fechaFinEstimada > :fechaInicio
    """)
    Long sumarAsistentesReservasSolapadas(
            @Param("salonId") Long salonId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );
}
