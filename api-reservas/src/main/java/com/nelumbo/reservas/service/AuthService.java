package com.nelumbo.reservas.service;

import com.nelumbo.reservas.dto.request.LoginRequest;
import com.nelumbo.reservas.dto.request.RegisterRequest;
import com.nelumbo.reservas.dto.response.AuthResponse;
import com.nelumbo.reservas.dto.response.UsuarioResponse;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.Rol;
import com.nelumbo.reservas.exception.EmailYaRegistradoException;
import com.nelumbo.reservas.repository.UsuarioRepository;
import com.nelumbo.reservas.security.JwtService;
import com.nelumbo.reservas.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + request.email()));

        log.info("Login exitoso para usuario: {}", usuario.getEmail());

        return AuthResponse.bearer(token, usuario.getEmail(), usuario.getNombre(), usuario.getRol());
    }

    public UsuarioResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new EmailYaRegistradoException(request.email());
        }

        Usuario nuevoGestor = Usuario.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nombre(request.nombre())
                .rol(Rol.GESTOR)
                .activo(true)
                .build();

        Usuario guardado = usuarioRepository.save(nuevoGestor);
        log.info("Nuevo GESTOR registrado: {}", guardado.getEmail());

        return UsuarioResponse.from(guardado);
    }

    public void logout(String token) {
        Date expiracion = jwtService.extractExpiration(token);
        tokenBlacklistService.revocar(token, expiracion.toInstant());
        log.info("Logout exitoso, token revocado");
    }


}
