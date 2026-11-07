package com.franco.opsengine.audit.adapter.persistence;

import com.franco.opsengine.audit.application.DecisionRecordRepository;
import com.franco.opsengine.audit.domain.DecisionRecord;
import com.franco.opsengine.audit.domain.JudgementSnapshot;
import com.franco.opsengine.routing.domain.CandidateEvaluation;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.ticket.domain.TicketId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class DecisionRecordRepositoryAdapter implements DecisionRecordRepository {

    private final DecisionRecordJpaRepository jpa;

    DecisionRecordRepositoryAdapter(DecisionRecordJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void record(DecisionRecord decision) {
        JudgementSnapshot judgement = decision.judgement();
        DecisionRecordEntity entity = new DecisionRecordEntity(
                decision.id(),
                decision.ticketId().value(),
                decision.provider(),
                decision.policyVersion(),
                judgement == null ? null : judgement.suggestedSkill(),
                judgement == null ? null : judgement.skillConfidence(),
                judgement == null ? null : judgement.skillRecognized(),
                judgement == null ? null : judgement.urgency(),
                judgement == null ? null : judgement.urgencyLevels(),
                judgement == null ? null : judgement.multipleCustomersAffected(),
                judgement == null ? null : judgement.serviceUnavailable(),
                decision.outcome(),
                decision.assignee().map(EmployeeId::value).orElse(null),
                decision.reviewReason(),
                decision.rationale(),
                decision.latencyMs(),
                decision.decidedAt());

        decision.candidates().forEach(candidate -> entity.addCandidate(new DecisionCandidateEntity(
                candidate.employeeId().value(),
                candidate.employeeName(),
                candidate.eligible(),
                candidate.rejectionReason(),
                candidate.openTickets(),
                candidate.maxOpenTickets())));

        jpa.save(entity);
    }

    @Override
    public List<DecisionRecord> findByTicket(TicketId ticketId) {
        return jpa.findByTicketIdOrderByDecidedAtDesc(ticketId.value()).stream()
                .map(DecisionRecordRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<DecisionRecord> findLatestByTicket(TicketId ticketId) {
        return findByTicket(ticketId).stream().findFirst();
    }

    @Override
    public long countAll() {
        return jpa.count();
    }

    @Override
    public long countByOutcome(DecisionRecord.Outcome outcome) {
        return jpa.countByOutcome(outcome);
    }

    private static DecisionRecord toDomain(DecisionRecordEntity entity) {
        JudgementSnapshot judgement = entity.getSuggestedSkill() == null ? null : new JudgementSnapshot(
                entity.getSuggestedSkill(),
                entity.getSkillConfidence(),
                entity.getSkillRecognized(),
                entity.getUrgency(),
                entity.getUrgencyLevels(),
                entity.getMultipleCustomers(),
                entity.getServiceUnavailable());

        List<CandidateEvaluation> candidates = entity.getCandidates().stream()
                .map(candidate -> new CandidateEvaluation(
                        new EmployeeId(candidate.getEmployeeId()),
                        candidate.getEmployeeName(),
                        candidate.isEligible(),
                        candidate.getRejectionReason(),
                        candidate.getOpenTickets(),
                        candidate.getMaxOpenTickets()))
                .toList();

        return new DecisionRecord(
                entity.getId(),
                new TicketId(entity.getTicketId()),
                entity.getProvider(),
                entity.getPolicyVersion(),
                judgement,
                entity.getOutcome(),
                entity.getAssignedEmployeeId() == null ? null : new EmployeeId(entity.getAssignedEmployeeId()),
                entity.getReviewReason(),
                entity.getRationale(),
                candidates,
                entity.getLatencyMs(),
                entity.getDecidedAt());
    }
}
