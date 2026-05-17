package com.nelumbo.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ApiReservasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiReservasApplication.class, args);
    }

}
