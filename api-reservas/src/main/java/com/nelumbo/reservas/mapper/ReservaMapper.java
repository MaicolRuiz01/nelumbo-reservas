package com.nelumbo.reservas.mapper;

import com.nelumbo.reservas.dto.response.ReservaResponse;
import com.nelumbo.reservas.entity.Reserva;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ReservaMapper {

    @Mapping(source = "salon.id",     target = "salonId")
    @Mapping(source = "salon.nombre", target = "salonNombre")
    @Mapping(source = "gestor.id",    target = "gestorId")
    @Mapping(source = "gestor.nombre", target = "gestorNombre")
    ReservaResponse toResponse(Reserva reserva);
}
