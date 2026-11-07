package com.franco.opsengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class OpsEngineApplication {

    public static void main(String[] args) {
        // Toda la aplicación corre en UTC: es lo que se guarda en la base y lo que el driver
        // de PostgreSQL le informa a la conexión. La zona del usuario se aplica al presentar.
        // Ver ADR-004.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(OpsEngineApplication.class, args);
    }
}
