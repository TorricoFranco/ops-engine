package com.franco.opsengine.evaluation.domain;

import com.franco.opsengine.routing.domain.RoutingCandidate;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * El equipo sintético contra el que se evalúa, con la carga acumulándose ticket a ticket.
 *
 * <p>Todos están disponibles, habilitados para incidentes críticos y con capacidad de sobra.
 * Es a propósito: si la capacidad o la habilitación frenaran asignaciones, el resultado
 * mediría eso y no lo que se quiere medir, que es qué tan bien se elige la competencia.
 */
public final class EvaluationRoster {

    private static final int CAPACITY = 1000;

    private final List<Agent> agents;

    private EvaluationRoster(List<Agent> agents) {
        this.agents = agents;
    }

    private static final class Agent {
        private final EmployeeId id;
        private final String name;
        private final Skill skill;
        private int load;

        private Agent(EmployeeId id, String name, Skill skill) {
            this.id = id;
            this.name = name;
            this.skill = skill;
        }
    }

    /** Un agente por competencia. Los ids son fijos para que una corrida sea reproducible. */
    public static EvaluationRoster forSkills(List<Skill> skills) {
        List<Agent> agents = new ArrayList<>();
        int index = 1;
        for (Skill skill : skills) {
            agents.add(new Agent(
                    new EmployeeId(UUID.nameUUIDFromBytes(("eval-" + skill.code()).getBytes())),
                    "Agente de " + skill.code(),
                    skill));
            index++;
        }
        return new EvaluationRoster(agents);
    }

    public List<RoutingCandidate> candidatesFor(Skill required) {
        return agents.stream()
                .filter(agent -> agent.skill.equals(required))
                .map(EvaluationRoster::toCandidate)
                .toList();
    }

    public List<RoutingCandidate> allCandidates() {
        return agents.stream().map(EvaluationRoster::toCandidate).toList();
    }

    /** El de menos carga, sin mirar competencias. Es la línea base sin inteligencia. */
    public Optional<EmployeeId> leastLoaded() {
        return agents.stream()
                .min(Comparator.<Agent>comparingInt(agent -> agent.load)
                        .thenComparing(agent -> agent.id.value()))
                .map(agent -> agent.id);
    }

    public void recordAssignment(EmployeeId employeeId) {
        agents.stream()
                .filter(agent -> agent.id.equals(employeeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown agent: " + employeeId.value()))
                .load++;
    }

    public Optional<Skill> skillOf(EmployeeId employeeId) {
        return agents.stream()
                .filter(agent -> agent.id.equals(employeeId))
                .findFirst()
                .map(agent -> agent.skill);
    }

    public Map<String, Integer> loads() {
        return agents.stream().collect(java.util.stream.Collectors.toMap(
                agent -> agent.name, agent -> agent.load));
    }

    private static RoutingCandidate toCandidate(Agent agent) {
        return new RoutingCandidate(agent.id, agent.name, java.util.Set.of(agent.skill),
                true, agent.load, CAPACITY, true);
    }
}
