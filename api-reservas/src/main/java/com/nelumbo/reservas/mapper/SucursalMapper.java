package com.nelumbo.reservas.mapper;

import com.nelumbo.reservas.dto.request.SucursalRequest;
import com.nelumbo.reservas.dto.response.SucursalResponse;
import com.nelumbo.reservas.entity.Sucursal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SucursalMapper {

    @Mapping(target = "gestor", ignore = true)
    Sucursal toEntity(SucursalRequest request);

    @Mapping(source = "gestor.id",     target = "gestorId")
    @Mapping(source = "gestor.nombre", target = "gestorNombre")
    SucursalResponse toResponse(Sucursal sucursal);
}
