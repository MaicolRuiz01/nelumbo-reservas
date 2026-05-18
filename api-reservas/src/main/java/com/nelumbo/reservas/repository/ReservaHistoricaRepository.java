package com.nelumbo.reservas.repository;

import com.nelumbo.reservas.dto.response.TopClienteResponse;
import com.nelumbo.reservas.entity.ReservaHistorica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
