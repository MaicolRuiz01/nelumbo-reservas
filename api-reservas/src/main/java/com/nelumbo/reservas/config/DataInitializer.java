package com.nelumbo.reservas.config;

import com.nelumbo.reservas.entity.Usuario;
import com.nelumbo.reservas.enums.Rol;
import com.nelumbo.reservas.repository.UsuarioRepository;
import com.nelumbo.reservas.security.JwtService;
import com.nelumbo.reservas.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedAdmin(){
        return args -> {
            final String emailAdmin = "admin@mail.com";

            if (usuarioRepository.existsByEmail(emailAdmin)){
                log.info("Usuario admin ya existe, se omite la creacion");
                return;
            }

            Usuario admin = Usuario.builder()
                    .email(emailAdmin)
                    .password(passwordEncoder.encode("admin"))
                    .nombre("Administrador")
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .build();

            usuarioRepository.save(admin);
            log.info("Usuario admin creado correctamente: {}", emailAdmin);
        };
    }
}
