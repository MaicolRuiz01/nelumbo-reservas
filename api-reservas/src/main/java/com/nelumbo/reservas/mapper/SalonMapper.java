package com.nelumbo.reservas.mapper;

import com.nelumbo.reservas.dto.request.SalonRequest;
import com.nelumbo.reservas.dto.response.SalonResponse;
import com.nelumbo.reservas.entity.Salon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper
public interface SalonMapper {

    @Mapping(target = "sucursal", ignore = true)
    Salon toEntity(SalonRequest request);

    @Mapping(source = "sucursal.id",            target = "sucursalId")
    @Mapping(source = "sucursal.nombre",         target = "sucursalNombre")
    @Mapping(source = "sucursal.gestor.id",      target = "gestorId")
    @Mapping(source = "sucursal.gestor.nombre",  target = "gestorNombre")
    SalonResponse toResponse(Salon salon);
}
