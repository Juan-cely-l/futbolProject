package futbol.api.com.external;

import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.team.TeamData;

import java.util.List;

public interface FootballApiProvider{
    List<TeamData>getTeamsByLeague(Integer leagueId , Integer season);
    List<PlayerInfo>getPlayersByTeam(Integer teamId,Integer season,Integer leagueId);
}
