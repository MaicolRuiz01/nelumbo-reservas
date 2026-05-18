package com.nelumbo.reservas.repository;

import com.nelumbo.reservas.entity.ReservaHistorica;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaHistoricaRepository
        extends JpaRepository<ReservaHistorica, Long> {
}
