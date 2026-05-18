package com.nelumbo.reservas.service;

import com.nelumbo.reservas.dto.response.TopClienteResponse;
import com.nelumbo.reservas.entity.Salon;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.exception.AccesoDenegadoException;
import com.nelumbo.reservas.exception.SalonNoEncontradoException;
import com.nelumbo.reservas.mapper.ReservaMapper;
import com.nelumbo.reservas.repository.ReservaHistoricaRepository;
import com.nelumbo.reservas.repository.ReservaRepository;
import com.nelumbo.reservas.repository.SalonRepository;
import com.nelumbo.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicadorService {

    private final ReservaRepository reservaRepository;
    private final ReservaHistoricaRepository reservaHistoricaRepository;
    private final SalonRepository salonRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaMapper reservaMapper;

    // === Helpers privados que vamos a usar en varios bloques ===

    private Salon buscarSalonOFallar(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new SalonNoEncontradoException(salonId));
    }

    private void validarGestorEsDuenoDelSalon(String email, Salon salon) {
        Usuario gestor = obtenerUsuarioPorEmail(email);
        if (!salon.getGestor().getId().equals(gestor.getId())) {
            throw new AccesoDenegadoException("No tienes acceso a este salón");
        }
    }

    private Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    @Transactional(readOnly = true)
    public List<TopClienteResponse> topClientesGlobal() {

        List<TopClienteResponse> historicas =
                reservaHistoricaRepository.contarReservasPorClienteGlobal();

        List<TopClienteResponse> activas =
                reservaRepository.contarReservasActivasPorClienteGlobal();

        Map<String, TopClienteResponse> combinado = new HashMap<>();

        Stream.concat(historicas.stream(), activas.stream()).forEach(tc ->
                combinado.merge(
                        tc.documentoCliente(),
                        tc,
                        (existente, nuevo) -> new TopClienteResponse(
                                existente.documentoCliente(),
                                existente.nombreCliente(),
                                existente.cantidad() + nuevo.cantidad()
                        )
                )
        );

        return combinado.values().stream()
                .sorted(Comparator.comparingLong(TopClienteResponse::cantidad).reversed())
                .limit(10)
                .toList();
    }
}