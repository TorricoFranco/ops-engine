package com.franco.opsengine.team.application;

import com.franco.opsengine.shared.domain.TeamId;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(TeamId id) {
        super("team not found: " + id.value());
    }
}
