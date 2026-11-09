package com.franco.opsengine;

import com.franco.opsengine.evaluation.adapter.cli.EvaluationCli;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.Arrays;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class OpsEngineApplication {

    public static void main(String[] args) {
        // Toda la aplicación corre en UTC: es lo que se guarda en la base y lo que el driver
        // de PostgreSQL le informa a la conexión. La zona del usuario se aplica al presentar.
        // Ver ADR-004.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // La evaluación offline es una simulación en memoria: no necesita el contexto de
        // Spring ni la base, así que corta acá antes de levantarlos.
        if (Arrays.asList(args).contains("--evaluate")) {
            System.exit(EvaluationCli.run(args, Clock.systemUTC()));
        }

        SpringApplication.run(OpsEngineApplication.class, args);
    }
}
