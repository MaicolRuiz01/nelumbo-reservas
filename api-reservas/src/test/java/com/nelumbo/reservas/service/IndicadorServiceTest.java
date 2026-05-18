package com.nelumbo.reservas.service;

import com.nelumbo.reservas.entity.Salon;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.Rol;
import com.nelumbo.reservas.exception.AccesoDenegadoException;
import com.nelumbo.reservas.mapper.ReservaMapper;
import com.nelumbo.reservas.repository.ReservaHistoricaRepository;
import com.nelumbo.reservas.repository.ReservaRepository;
import com.nelumbo.reservas.repository.SalonRepository;
import com.nelumbo.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class IndicadorServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private ReservaHistoricaRepository reservaHistoricaRepository;
    @Mock private SalonRepository salonRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ReservaMapper reservaMapper;

    @InjectMocks private IndicadorService indicadorService;

    @Test
    @DisplayName("gananciasPorSalon: un GESTOR que no es dueño del salón recibe AccesoDenegadoException y NO se consulta facturación")
    void gananciasPorSalon_gestorNoEsDuenoDelSalon_lanzaAccesoDenegado() {
        // Arrange — el salón pertenece al gestor "dueno@mail.com"
        Usuario dueno = new Usuario();
        dueno.setId(10L);
        dueno.setEmail("dueno@mail.com");
        dueno.setRol(Rol.GESTOR);

        Salon salon = new Salon();
        salon.setId(1L);
        salon.setNombre("Salón Cristal");
        salon.setCapacidad(50);
        salon.setCostoPorHora(BigDecimal.valueOf(100_000));
        salon.setGestor(dueno);

        // El gestor que hace la consulta es OTRO usuario distinto
        Usuario otroGestor = new Usuario();
        otroGestor.setId(20L);
        otroGestor.setEmail("intruso@mail.com");
        otroGestor.setRol(Rol.GESTOR);

        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(usuarioRepository.findByEmail("intruso@mail.com"))
                .thenReturn(Optional.of(otroGestor));

        // Act + Assert — esAdmin=false → se ejecuta la validación de ownership
        assertThatThrownBy(() ->
                indicadorService.gananciasPorSalon(1L, "intruso@mail.com", false))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("No tienes acceso a este salón");

        // No se debió consultar la facturación: el corte de seguridad ocurre antes
        verify(reservaHistoricaRepository, never())
                .sumarTotalCobradoPorRango(any(), any(), any());
    }
}
