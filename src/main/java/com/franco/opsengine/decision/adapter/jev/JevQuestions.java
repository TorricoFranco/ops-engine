package com.franco.opsengine.decision.adapter.jev;

import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.shared.domain.Skill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Todo el texto que ve el modelo vive acá: el estado, las preguntas y los criterios. Es la
 * parte que más se audita y se ajusta, así que se mantiene junta y fuera del código de
 * transporte.
 *
 * <p>Ninguna pregunta le pide al modelo que decida. No hay un "¿hay que escalar esto?": eso
 * depende del SLA, de los permisos y de la carga del equipo, que son datos del sistema. Las
 * preguntas piden juicios observables, y las reglas los combinan (ADR-005).
 */
final class JevQuestions {

    static final String SKILL = "required_skill";
    static final String SKILL_RECOGNIZED = "skill_recognized";
    static final String URGENCY = "urgency";
    static final String MULTIPLE_CUSTOMERS = "multiple_customers_affected";
    static final String SERVICE_UNAVAILABLE = "service_unavailable";

    /** Los niveles de urgencia, descritos como situaciones para que cada uno se lea solo. */
    static final List<String> URGENCY_LEVELS = List.of(
            "El cliente pide información o hace una consulta que puede esperar.",
            "El cliente reporta una molestia menor y sigue pudiendo trabajar.",
            "El cliente no puede completar una tarea, pero tiene una alternativa.",
            "El cliente no puede operar y no menciona ninguna alternativa.",
            "El cliente describe una interrupción que ya está afectando a su negocio.");

    private JevQuestions() {
    }

    /** Objetos con campos nombrados, para que cada pregunta pueda referirse a `ticket.titulo`. */
    static Map<String, Object> state(TicketContext context) {
        return Map.of("ticket", Map.of(
                "titulo", context.title(),
                "descripcion", context.description()));
    }

    static Map<String, JevRequest.Question> questions(TicketContext context) {
        Map<String, String> skillCriteria = new LinkedHashMap<>();
        context.candidateSkills().forEach((skill, description) -> skillCriteria.put(skill.code(), description));

        Map<String, JevRequest.Question> questions = new LinkedHashMap<>();

        questions.put(SKILL, JevRequest.Question.choice(
                "¿Qué competencia hace falta para resolver lo que describe `ticket`?",
                skillCriteria));

        // Un choice siempre elige una opción, incluso cuando ninguna encaja. Esta pregunta
        // detecta ese caso para que el ruteo lo mande a revisión en vez de forzar una.
        questions.put(SKILL_RECOGNIZED, JevRequest.Question.noul(
                "¿Lo que describe `ticket` corresponde a alguna de las competencias del equipo?",
                Map.of(
                        "true", "El problema entra claramente en una de las áreas de soporte listadas.",
                        "false", "El problema es de otra área, o el texto no alcanza para ubicarlo.")));

        questions.put(URGENCY, JevRequest.Question.score(
                "¿Cuánto le impide operar al cliente lo que describe `ticket`?",
                URGENCY_LEVELS));

        questions.put(MULTIPLE_CUSTOMERS, JevRequest.Question.noul(
                "¿`ticket` dice que el problema afecta a más de un cliente o cuenta?",
                Map.of(
                        "true", "El texto menciona varios clientes, cuentas o usuarios afectados.",
                        "false", "El texto describe el problema de una sola cuenta.")));

        questions.put(SERVICE_UNAVAILABLE, JevRequest.Question.noul(
                "¿`ticket` dice que un servicio está caído o no responde?",
                Map.of(
                        "true", "El texto indica que el servicio no responde, devuelve errores o está caído.",
                        "false", "El servicio responde, aunque su comportamiento sea incorrecto.")));

        return questions;
    }

    static Skill skillOf(String code) {
        return new Skill(code);
    }
}
