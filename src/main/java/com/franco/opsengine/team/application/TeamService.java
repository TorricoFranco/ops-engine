package com.franco.opsengine.team.application;

import com.franco.opsengine.shared.domain.TeamId;
import com.franco.opsengine.team.domain.Team;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teams;

    public TeamService(TeamRepository teams) {
        this.teams = teams;
    }

    public Team create(String name) {
        return teams.save(Team.create(TeamId.newId(), name));
    }

    public Team byId(TeamId id) {
        return teams.findById(id).orElseThrow(() -> new TeamNotFoundException(id));
    }

    public List<Team> all() {
        return teams.findAll();
    }
}
