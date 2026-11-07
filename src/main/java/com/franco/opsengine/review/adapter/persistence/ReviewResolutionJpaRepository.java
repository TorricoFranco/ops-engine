package com.franco.opsengine.review.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ReviewResolutionJpaRepository extends JpaRepository<ReviewResolutionEntity, UUID> {

    List<ReviewResolutionEntity> findByTicketIdOrderByResolvedAtDesc(UUID ticketId);

    long countByAgreedWithSuggestionIsFalse();

    long countByAgreedWithSuggestionIsNotNull();
}
