package com.nelumbo.reservas.repository;

import com.nelumbo.reservas.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    List<Sucursal> findByGestorId(Long gestorId);
}