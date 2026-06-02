package futbol.api.com.services.Player;

import futbol.api.com.dtos.player.CreatePlayerRequest;
import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.player.UpdatePlayerRequest;
import futbol.api.com.dtos.team.PlayerEfficiencyResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface PlayerService {

    PlayerResponse createPlayer(CreatePlayerRequest request);
    PlayerResponse getPlayerById(UUID id);
    PlayerResponse updatePlayer(UUID id, UpdatePlayerRequest request);
    PlayerEfficiencyResponse getEfficiency(UUID id);
    Page<PlayerResponse> getAllPlayers(int page, int size, String sortBy, String sortDir, String search);
    void deletePlayer(UUID id);

}
