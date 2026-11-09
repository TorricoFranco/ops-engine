package com.franco.opsengine.evaluation.adapter.cli;

import com.franco.opsengine.decision.adapter.fake.FakeDecisionProvider;
import com.franco.opsengine.decision.adapter.jev.JevProperties;
import com.franco.opsengine.decision.adapter.jev.JevProviderFactory;
import com.franco.opsengine.evaluation.application.LeastLoadedStrategy;
import com.franco.opsengine.evaluation.application.ProviderStrategy;
import com.franco.opsengine.evaluation.domain.EvaluationRoster;
import com.franco.opsengine.evaluation.domain.EvaluationStrategy;
import com.franco.opsengine.evaluation.domain.Evaluator;
import com.franco.opsengine.evaluation.domain.LabelledTicket;
import com.franco.opsengine.routing.domain.RoutingPolicy;
import com.franco.opsengine.shared.domain.Skill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Corre la evaluación offline sin levantar Spring ni tocar la base de datos.
 *
 * <p>No arranca el contexto a propósito: la evaluación es una simulación en memoria y no
 * debería necesitar PostgreSQL ni dejar rastro en él. Entra por el main de la aplicación con
 * el argumento {@code --evaluate}, así no hace falta un plugin de build ni otro ejecutable.
 */
public final class EvaluationCli {

    /**
     * Las competencias con su descripción, igual que en la migración V2.
     *
     * <p>Está duplicado a propósito: traerlas de la base obligaría a levantar el contexto para
     * una herramienta que no lo necesita. Si cambian en V2, hay que cambiarlas acá.
     */
    private static final Map<Skill, String> CATALOG = new LinkedHashMap<>();

    static {
        CATALOG.put(new Skill("ACCESS_IDENTITY"),
                "Inicio de sesión, contraseñas, permisos y accesos de usuarios.");
        CATALOG.put(new Skill("BILLING"),
                "Cobros, facturas, reembolsos y problemas con los pagos.");
        CATALOG.put(new Skill("INTEGRATIONS"),
                "Sincronización, webhooks y conexiones con sistemas externos.");
        CATALOG.put(new Skill("INFRASTRUCTURE"),
                "Caídas del servicio, lentitud, errores del servidor y capacidad.");
    }

    /** Los mismos umbrales que trae application.yml por defecto. */
    private static final RoutingPolicy POLICY = new RoutingPolicy(
            "2026-11-07.1", 0.7, 0.6, 0.7, 0.6, Duration.ofMinutes(30));

    private EvaluationCli() {
    }

    /** @return código de salida: 0 si la evaluación corrió, 1 si el dataset no sirve */
    public static int run(String[] args, Clock clock) {
        Path datasetPath = Path.of(argumentOr(args, "--dataset", "docs/evaluation/dataset.csv"));
        Path reportPath = Path.of(argumentOr(args, "--report", "docs/evaluation/report.md"));

        List<LabelledTicket> dataset;
        try {
            dataset = DatasetReader.read(Files.readString(datasetPath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("No se pudo leer el dataset en " + datasetPath.toAbsolutePath());
            System.err.println("Copiá docs/evaluation/dataset.example.csv y etiquetá los casos.");
            return 1;
        } catch (DatasetReader.InvalidDatasetException e) {
            System.err.println("El dataset no sirve para evaluar:");
            System.err.println(e.getMessage());
            return 1;
        }

        System.out.println("Evaluando " + dataset.size() + " casos de " + datasetPath);
        if (dataset.size() < 30) {
            // No se frena, pero se avisa: con pocos casos un porcentaje se mueve demasiado.
            System.out.println("Aviso: con menos de 30 casos los porcentajes no son estables.");
        }

        List<EvaluationStrategy> strategies = new ArrayList<>(List.of(
                new LeastLoadedStrategy(),
                new ProviderStrategy("Palabras clave + reglas", new FakeDecisionProvider(),
                        CATALOG, POLICY)));

        // Jev solo entra si hay clave. Sin ella el reporte sale igual, con una estrategia
        // menos, en lugar de fallar: la evaluación tiene que poder correrse sin gastar.
        String apiKey = System.getenv("TYPESAFE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("Sin TYPESAFE_API_KEY: se evalúa sin Jev.");
        } else {
            System.out.println("Con TYPESAFE_API_KEY: se incluye Jev ("
                    + dataset.size() + " llamadas a la API).");
            strategies.add(new ProviderStrategy("Jev + reglas",
                    JevProviderFactory.create(jevProperties(apiKey)), CATALOG, POLICY));
        }

        List<Evaluator.Run> runs = new ArrayList<>();
        for (EvaluationStrategy strategy : strategies) {
            // Equipo nuevo por estrategia: si compartieran la carga, la segunda arrancaría
            // con el reparto que dejó la primera.
            runs.add(Evaluator.run(strategy, dataset, EvaluationRoster.forSkills(
                    List.copyOf(CATALOG.keySet()))));
        }

        String report = MarkdownReport.render(runs, dataset.size(), clock.instant());
        try {
            Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, report, StandardCharsets.UTF_8);
            System.out.println("Reporte escrito en " + reportPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("No se pudo escribir el reporte: " + e.getMessage());
            return 1;
        }

        System.out.println();
        System.out.println(report);
        return 0;
    }

    /** Mismos valores por defecto que application.yml, salvo un timeout más holgado. */
    private static JevProperties jevProperties(String apiKey) {
        return new JevProperties("https://api.typesafe.ai", apiKey, "jev-latest",
                Duration.ofSeconds(5), Duration.ofSeconds(30), 3);
    }

    private static String argumentOr(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return fallback;
    }
}
