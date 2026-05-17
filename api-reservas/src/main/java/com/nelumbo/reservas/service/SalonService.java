package com.nelumbo.reservas.service;

import com.nelumbo.reservas.dto.request.SalonRequest;
import com.nelumbo.reservas.dto.response.SalonResponse;
import com.nelumbo.reservas.entity.Salon;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.Rol;
import com.nelumbo.reservas.exception.AccesoDenegadoException;
import com.nelumbo.reservas.exception.GestorNoEncontradoException;
import com.nelumbo.reservas.exception.SalonNoEncontradoException;
import com.nelumbo.reservas.exception.SucursalNoEncontradaException;
import com.nelumbo.reservas.exception.UsuarioNoEsGestorException;
import com.nelumbo.reservas.mapper.SalonMapper;
import com.nelumbo.reservas.repository.SalonRepository;
import com.nelumbo.reservas.repository.SucursalRepository;
import com.nelumbo.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalonService {

    private final SalonRepository salonRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final SalonMapper salonMapper;

    @Transactional
    public SalonResponse crear(SalonRequest request) {
        Salon salon = salonMapper.toEntity(request);
        salon.setSucursal(sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new SucursalNoEncontradaException(request.sucursalId())));
        salon.setGestor(buscarGestorValidoOFallar(request.gestorId()));
        return salonMapper.toResponse(salonRepository.save(salon));
    }

    @Transactional(readOnly = true)
    public List<SalonResponse> listar(String email, boolean esAdmin) {
        if (esAdmin) {
            return salonRepository.findAll()
                    .stream().map(salonMapper::toResponse).toList();
        }
        Usuario gestor = obtenerUsuarioPorEmail(email);
        return salonRepository.findByGestorId(gestor.getId())
                .stream().map(salonMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalonResponse obtenerPorId(Long id, String email, boolean esAdmin) {
        Salon salon = buscarOFallar(id);
        if (!esAdmin) {
            Usuario gestor = obtenerUsuarioPorEmail(email);
            if (!salon.getGestor().getId().equals(gestor.getId())) {
                throw new AccesoDenegadoException("No tienes acceso a este salón");
            }
        }
        return salonMapper.toResponse(salon);
    }

    @Transactional
    public SalonResponse actualizar(Long id, SalonRequest request) {
        Salon salon = buscarOFallar(id);
        salon.setNombre(request.nombre());
        salon.setCapacidad(request.capacidad());
        salon.setCostoPorHora(request.costoPorHora());
        salon.setSucursal(sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new SucursalNoEncontradaException(request.sucursalId())));
        salon.setGestor(buscarGestorValidoOFallar(request.gestorId()));
        return salonMapper.toResponse(salonRepository.save(salon));
    }

    @Transactional
    public void eliminar(Long id) {
        buscarOFallar(id);
        salonRepository.deleteById(id);
    }

    private Salon buscarOFallar(Long id) {
        return salonRepository.findById(id)
                .orElseThrow(() -> new SalonNoEncontradoException(id));
    }

    private Usuario buscarGestorValidoOFallar(Long gestorId) {
        Usuario gestor = usuarioRepository.findById(gestorId)
                .orElseThrow(() -> new GestorNoEncontradoException(gestorId));
        if (gestor.getRol() != Rol.GESTOR) {
            throw new UsuarioNoEsGestorException(gestorId);
        }
        return gestor;
    }

    private Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }
}
