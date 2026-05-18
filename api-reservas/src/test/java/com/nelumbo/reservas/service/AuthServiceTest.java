package com.nelumbo.reservas.service;

import com.nelumbo.reservas.dto.request.RegisterRequest;
import com.nelumbo.reservas.dto.response.UsuarioResponse;
import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.Rol;
import com.nelumbo.reservas.exception.EmailYaRegistradoException;
import com.nelumbo.reservas.repository.UsuarioRepository;
import com.nelumbo.reservas.security.JwtService;
import com.nelumbo.reservas.security.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("register: si el email ya existe, lanza EmailYaRegistradoException y no guarda nada")
    void register_cuandoEmailYaExiste_lanzaExcepcionYNoGuarda() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "duplicado@mail.com", "secreta123", "Gestor Duplicado");
        when(usuarioRepository.existsByEmail("duplicado@mail.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailYaRegistradoException.class)
                .hasMessageContaining("duplicado@mail.com");

        // Nunca se guardó nada ni se llamó al encoder
        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("register: con email nuevo, persiste un GESTOR activo con la contraseña hasheada")
    void register_cuandoEmailEsNuevo_persisteGestorConPasswordHasheada() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "nuevo@mail.com", "secreta123", "Gestor Nuevo");

        when(usuarioRepository.existsByEmail("nuevo@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secreta123")).thenReturn("$2a$bcrypt-hash");

        // Simulamos el save devolviendo el mismo Usuario con id asignado
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(42L);
            u.setFechaCreacion(LocalDateTime.now());
            return u;
        });

        // Act
        UsuarioResponse response = authService.register(request);

        // Assert — capturamos el Usuario que se guardó para inspeccionar sus campos
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();

        assertThat(guardado.getEmail()).isEqualTo("nuevo@mail.com");
        assertThat(guardado.getNombre()).isEqualTo("Gestor Nuevo");
        assertThat(guardado.getRol())
                .as("El PDF (sección 2) dice que register solo crea GESTORES")
                .isEqualTo(Rol.GESTOR);
        assertThat(guardado.getActivo()).isTrue();
        assertThat(guardado.getPassword())
                .as("La contraseña debe guardarse hasheada, nunca en claro")
                .isEqualTo("$2a$bcrypt-hash")
                .isNotEqualTo("secreta123");

        // El response refleja el usuario guardado
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("nuevo@mail.com");
        assertThat(response.rol()).isEqualTo(Rol.GESTOR);
    }
}
