package com.franco.opsengine.team.adapter.web;

import com.franco.opsengine.team.application.TeamService;
import com.franco.opsengine.team.domain.Team;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@Tag(name = "Equipos")
class TeamController {

    private final TeamService teams;

    TeamController(TeamService teams) {
        this.teams = teams;
    }

    record CreateTeamRequest(@NotBlank String name) {
    }

    record TeamResponse(UUID id, String name) {
        static TeamResponse from(Team team) {
            return new TeamResponse(team.id().value(), team.name());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea un equipo (solo ADMIN)")
    ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        Team team = teams.create(request.name());
        return ResponseEntity
                .created(URI.create("/api/teams/" + team.id().value()))
                .body(TeamResponse.from(team));
    }

    @GetMapping
    @Operation(summary = "Lista los equipos")
    List<TeamResponse> all() {
        return teams.all().stream().map(TeamResponse::from).toList();
    }
}
