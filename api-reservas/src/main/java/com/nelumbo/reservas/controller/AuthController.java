package com.nelumbo.reservas.controller;

import com.nelumbo.reservas.dto.request.LoginRequest;
import com.nelumbo.reservas.dto.request.RegisterRequest;
import com.nelumbo.reservas.dto.response.AuthResponse;
import com.nelumbo.reservas.dto.response.UsuarioResponse;
import com.nelumbo.reservas.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "Header Authorization invalido"));
        }

        String token = authHeader.substring(7);
        authService.logout(token);

        return ResponseEntity.ok(Map.of("mensaje", "Logout exitoso"));
    }
}
