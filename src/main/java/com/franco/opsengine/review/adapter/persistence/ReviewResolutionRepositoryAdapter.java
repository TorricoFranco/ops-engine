package com.franco.opsengine.review.adapter.persistence;

import com.franco.opsengine.review.application.ReviewResolutionRepository;
import com.franco.opsengine.review.domain.ReviewResolution;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.ticket.domain.TicketId;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class ReviewResolutionRepositoryAdapter implements ReviewResolutionRepository {

    private final ReviewResolutionJpaRepository jpa;

    ReviewResolutionRepositoryAdapter(ReviewResolutionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void record(ReviewResolution resolution) {
        jpa.save(new ReviewResolutionEntity(
                resolution.id(),
                resolution.ticketId().value(),
                resolution.decisionRecordId(),
                resolution.resolvedBy(),
                resolution.resolution(),
                resolution.assignee().map(EmployeeId::value).orElse(null),
                resolution.agreedWithSuggestion(),
                resolution.disagreementReason(),
                resolution.resolvedAt()));
    }

    @Override
    public List<ReviewResolution> findByTicket(TicketId ticketId) {
        return jpa.findByTicketIdOrderByResolvedAtDesc(ticketId.value()).stream()
                .map(entity -> new ReviewResolution(
                        entity.getId(),
                        new TicketId(entity.getTicketId()),
                        entity.getDecisionRecordId(),
                        entity.getResolvedBy(),
                        entity.getResolution(),
                        entity.getAssignedEmployeeId() == null
                                ? null : new EmployeeId(entity.getAssignedEmployeeId()),
                        entity.getAgreedWithSuggestion(),
                        entity.getDisagreementReason(),
                        entity.getResolvedAt()))
                .toList();
    }

    @Override
    public long countAll() {
        return jpa.count();
    }

    @Override
    public long countOverrides() {
        return jpa.countByAgreedWithSuggestionIsFalse();
    }

    @Override
    public long countWithSuggestion() {
        return jpa.countByAgreedWithSuggestionIsNotNull();
    }
}
