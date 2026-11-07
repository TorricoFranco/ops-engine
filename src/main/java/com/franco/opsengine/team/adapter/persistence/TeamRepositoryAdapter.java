package com.franco.opsengine.team.adapter.persistence;

import com.franco.opsengine.shared.domain.TeamId;
import com.franco.opsengine.team.application.TeamRepository;
import com.franco.opsengine.team.domain.Team;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class TeamRepositoryAdapter implements TeamRepository {

    private final TeamJpaRepository jpa;

    TeamRepositoryAdapter(TeamJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Team save(Team team) {
        return toDomain(jpa.saveAndFlush(new TeamEntity(team.id().value(), team.name())));
    }

    @Override
    public Optional<Team> findById(TeamId id) {
        return jpa.findById(id.value()).map(TeamRepositoryAdapter::toDomain);
    }

    @Override
    public List<Team> findAll() {
        return jpa.findAll().stream().map(TeamRepositoryAdapter::toDomain).toList();
    }

    private static Team toDomain(TeamEntity entity) {
        return Team.create(new TeamId(entity.getId()), entity.getName());
    }
}
