package futbol.api.com.services.Team;

import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.team.CreateTeamRequest;
import futbol.api.com.dtos.team.TeamResponse;
import futbol.api.com.dtos.team.TeamValueResponse;
import futbol.api.com.dtos.team.UpdateTeamRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface TeamService {
    TeamResponse createTeam(CreateTeamRequest request);
    TeamResponse updateTeam(UUID id, UpdateTeamRequest request);
    TeamResponse getTeamById(UUID id);
    TeamResponse getTeambyName(String name);
    Page<TeamResponse> getAllTeams(int page, int size, String sortBy, String sortDir, String search);
    List<PlayerResponse> getTeamSquad(String name);
    TeamValueResponse getTeamValue(String name);
    void deleteTeam(UUID id);
}
