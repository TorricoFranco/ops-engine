package com.franco.opsengine.employee.domain;

/**
 * Habilitación del empleado para incidentes críticos. Es una restricción de la empresa: el
 * modelo puede sugerir a quién mandar un ticket, pero no puede saltearse esto.
 */
public enum Clearance {
    STANDARD,
    CRITICAL;

    public boolean allowsCriticalIncidents() {
        return this == CRITICAL;
    }
}
