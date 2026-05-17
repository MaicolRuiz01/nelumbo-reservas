package com.nelumbo.reservas.service;

import com.nelumbo.reservas.dto.request.SucursalRequest;
import com.nelumbo.reservas.dto.response.SucursalResponse;
import com.nelumbo.reservas.entity.Sucursal;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.Rol;
import com.nelumbo.reservas.exception.*;
import com.nelumbo.reservas.mapper.SucursalMapper;
import com.nelumbo.reservas.repository.SucursalRepository;
import com.nelumbo.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final SucursalMapper sucursalMapper;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public SucursalResponse crear(SucursalRequest request) {
        Sucursal sucursal = sucursalMapper.toEntity(request);
        sucursal.setGestor(buscarGestorValidoOFallar(request.gestorId()));
        return sucursalMapper.toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional(readOnly = true)
    public List<SucursalResponse> listar(String email, boolean esAdmin) {
        if (esAdmin) {
            return sucursalRepository.findAll()
                    .stream().map(sucursalMapper::toResponse).toList();
        }
        Usuario gestor = obtenerUsuarioPorEmail(email);
        return sucursalRepository.findByGestorId(gestor.getId())
                .stream().map(sucursalMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SucursalResponse obtenerPorId(Long id, String email, boolean esAdmin) {
        Sucursal sucursal = buscarOFallar(id);
        if (!esAdmin) {
            Usuario gestor = obtenerUsuarioPorEmail(email);
            if (!sucursal.getGestor().getId().equals(gestor.getId())) {
                throw new AccesoDenegadoException("No tienes acceso a esta sucursal");
            }
        }
        return sucursalMapper.toResponse(sucursal);
    }

    @Transactional
    public SucursalResponse actualizar(Long id, SucursalRequest request) {
        Sucursal sucursal = buscarOFallar(id);
        sucursal.setNombre(request.nombre());
        sucursal.setCiudad(request.ciudad());
        sucursal.setDireccion(request.direccion());
        sucursal.setGestor(buscarGestorValidoOFallar(request.gestorId()));
        return sucursalMapper.toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional
    public void eliminar(Long id) {
        Sucursal sucursal = buscarOFallar(id);
        if (!sucursal.getSalones().isEmpty()) {
            throw new SucursalConSalonesException(id);
        }
        sucursalRepository.deleteById(id);
    }

    private Sucursal buscarOFallar(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new SucursalNoEncontradaException(id));
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