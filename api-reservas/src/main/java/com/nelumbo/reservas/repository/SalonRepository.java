package com.nelumbo.reservas.repository;

import com.nelumbo.reservas.entity.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    List<Salon> findBySucursalId(Long sucursalId);

    List<Salon> findByGestorId(Long gestorId);
}
