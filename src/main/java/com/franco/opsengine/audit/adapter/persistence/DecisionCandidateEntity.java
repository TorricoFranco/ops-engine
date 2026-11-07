package com.franco.opsengine.audit.adapter.persistence;

import com.franco.opsengine.routing.domain.RejectionReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "decision_candidates")
@IdClass(DecisionCandidateEntity.Key.class)
public class DecisionCandidateEntity {

    @Id
    @ManyToOne
    @JoinColumn(name = "decision_record_id", nullable = false)
    private DecisionRecordEntity decision;

    @Id
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "employee_name", nullable = false, length = 150)
    private String employeeName;

    @Column(nullable = false)
    private boolean eligible;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 50)
    private RejectionReason rejectionReason;

    @Column(name = "open_tickets", nullable = false)
    private int openTickets;

    @Column(name = "max_open_tickets", nullable = false)
    private int maxOpenTickets;

    protected DecisionCandidateEntity() {
        // requerido por JPA
    }

    DecisionCandidateEntity(UUID employeeId, String employeeName, boolean eligible,
                            RejectionReason rejectionReason, int openTickets, int maxOpenTickets) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.eligible = eligible;
        this.rejectionReason = rejectionReason;
        this.openTickets = openTickets;
        this.maxOpenTickets = maxOpenTickets;
    }

    void setDecision(DecisionRecordEntity decision) {
        this.decision = decision;
    }

    UUID getEmployeeId() {
        return employeeId;
    }

    String getEmployeeName() {
        return employeeName;
    }

    boolean isEligible() {
        return eligible;
    }

    RejectionReason getRejectionReason() {
        return rejectionReason;
    }

    int getOpenTickets() {
        return openTickets;
    }

    int getMaxOpenTickets() {
        return maxOpenTickets;
    }

    /** Clave compuesta: la decisión más el empleado. */
    public static class Key implements Serializable {

        private UUID decision;
        private UUID employeeId;

        public Key() {
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(decision, key.decision) && Objects.equals(employeeId, key.employeeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(decision, employeeId);
        }
    }
}
