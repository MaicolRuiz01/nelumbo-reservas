package com.nelumbo.reservas.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenBlacklistService {

    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void revocar(String token, Instant expiracion) {
        blacklist.put(token, expiracion);
        log.info("Token revocado. Tokens en blacklist: {}", blacklist.size());
    }

    public boolean estaRevocado(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * Se ejecuta cada 30 minutos y elimina los tokens cuya expiracion ya paso.
     * Asi la blacklist no crece indefinidamente.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void limpiarExpirados() {
        Instant ahora = Instant.now();
        int antes = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(ahora));
        int despues = blacklist.size();
        if (antes != despues) {
            log.info("Blacklist limpiada: {} tokens eliminados", antes - despues);
        }
    }
}
