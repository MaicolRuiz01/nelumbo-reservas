package com.nelumbo.reservas.repository;

import com.nelumbo.reservas.dto.response.TopClienteResponse;
import com.nelumbo.reservas.dto.response.TopSucursalResponse;
import com.nelumbo.reservas.entity.ReservaHistorica;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservaHistoricaRepository
        extends JpaRepository<ReservaHistorica, Long> {

    @Query("""
    SELECT new com.nelumbo.reservas.dto.response.TopClienteResponse(
        h.documentoCliente,
        h.nombreCliente,
        COUNT(h)
    )
    FROM ReservaHistorica h
    GROUP BY h.documentoCliente, h.nombreCliente
    """)
    List<TopClienteResponse> contarReservasPorClienteGlobal();

    @Query("""
    SELECT new com.nelumbo.reservas.dto.response.TopClienteResponse(
        h.documentoCliente,
        h.nombreCliente,
        COUNT(h)
    )
    FROM ReservaHistorica h
    WHERE h.salon.id = :salonId
    GROUP BY h.documentoCliente, h.nombreCliente
""")
    List<TopClienteResponse> contarReservasPorClienteEnSalon(Long salonId);

    @Query("""
    SELECT COALESCE(SUM(h.totalCobrado), 0)
    FROM ReservaHistorica h
    WHERE h.salon.id = :salonId
    AND h.fechaFinalizacionReal >= :desde
    AND h.fechaFinalizacionReal < :hasta
    """)
    BigDecimal sumarTotalCobradoPorRango(
            Long salonId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    @Query("""
    SELECT new com.nelumbo.reservas.dto.response.TopSucursalResponse(
        s.id,
        s.nombre,
        COALESCE(SUM(h.totalCobrado), 0)
    )
    FROM ReservaHistorica h
    JOIN h.salon sa
    JOIN sa.sucursal s
    WHERE h.fechaFinalizacionReal >= :desde
      AND h.fechaFinalizacionReal < :hasta
    GROUP BY s.id, s.nombre
    ORDER BY SUM(h.totalCobrado) DESC
""")
    List<TopSucursalResponse> topSucursalesPorFacturacion(
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable
    );
}
