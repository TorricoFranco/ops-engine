package com.franco.opsengine.team.application;

import com.franco.opsengine.shared.domain.TeamId;
import com.franco.opsengine.team.domain.Team;

import java.util.List;
import java.util.Optional;

public interface TeamRepository {

    Team save(Team team);

    Optional<Team> findById(TeamId id);

    List<Team> findAll();
}
