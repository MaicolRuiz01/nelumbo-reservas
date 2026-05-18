package com.nelumbo.reservas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notificaciones")
public class NotificacionesProperties {
    private String baseUrl;
    private int timeoutMs;
}
