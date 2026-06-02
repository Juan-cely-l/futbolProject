package futbol.api.com.services.Player;

import futbol.api.com.dtos.player.CreatePlayerRequest;
import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.player.UpdatePlayerRequest;
import futbol.api.com.dtos.team.PlayerEfficiencyResponse;
import futbol.api.com.exceptions.ResourceAlreadyExistsException;
import futbol.api.com.exceptions.ResourceNotFoundException;
import futbol.api.com.models.Player;
import futbol.api.com.models.Team;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final TeamRepository teamRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "goals", "position", "age", "assists", "matches", "valueMarket"
    );

    @Override
    @Transactional
    public PlayerResponse createPlayer(CreatePlayerRequest request) {
        Team team = teamRepository.findTeamByNameIgnoreCase(request.getTeamName().trim())
                .orElseThrow(() -> new ResourceNotFoundException("The team does not exist"));
        String nameNormalized = request.getName().toLowerCase().trim();
        String nameTeamNormalized = request.getTeamName().toLowerCase().trim();
        if (playerRepository.existsPlayerByNameAndAgeAndTeamName(nameNormalized, request.getAge(), nameTeamNormalized)) {
            throw new ResourceAlreadyExistsException("The Player already exists");
        }
        Player player = Player.builder()
                .name(nameNormalized)
                .goals(request.getGoals())
                .position(request.getPosition())
                .age(request.getAge())
                .assists(request.getAssists())
                .matches(request.getMatches())
                .valueMarket(request.getValueMarket())
                .team(team)
                .build();
        try {
            Player saved = playerRepository.save(player);
            return playerMapper.mapPlayerToResponseDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("The Player already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse getPlayerById(UUID id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + id));
        return playerMapper.mapPlayerToResponseDto(player);
    }

    @Override
    @Transactional
    public PlayerResponse updatePlayer(UUID id, UpdatePlayerRequest request) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("This player does not exist"));

        if (request.getAge() != null) {
            player.setAge(request.getAge());
        }
        if (request.getAssists() != null) {
            player.setAssists(request.getAssists());
        }
        if (request.getMatches() != null) {
            player.setMatches(request.getMatches());
        }
        if (request.getGoals() != null) {
            player.setGoals(request.getGoals());
        }
        if (request.getValueMarket() != null) {
            player.setValueMarket(request.getValueMarket());
        }

        if (request.getTeamName() != null && !request.getTeamName().isBlank()) {
            String teamNameNormalized = request.getTeamName().toLowerCase().trim();
            Team team = teamRepository.findTeamByNameIgnoreCase(teamNameNormalized)
                    .orElseThrow(() -> new ResourceNotFoundException("This team does not exist:" + request.getTeamName()));
            player.setTeam(team);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            player.setName(request.getName().toLowerCase().trim());
        }

        if (request.getPosition() != null) {
            player.setPosition(request.getPosition());
        }

        String name = player.getName();
        Integer age = player.getAge();
        String teamName = player.getTeam() != null ? player.getTeam().getName() : null;
        if (name != null && age != null && teamName != null
                && playerRepository.existsByNameAndAgeAndTeamNameAndIdNot(name, age, teamName, id)) {
            throw new ResourceAlreadyExistsException("The player already exists");
        }

        Player playerUpdated = playerRepository.save(player);
        return playerMapper.mapPlayerToResponseDto(playerUpdated);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerEfficiencyResponse getEfficiency(UUID id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("This player does not exist: " + id));
        int assists = player.getAssists() != null ? player.getAssists() : 0;
        int goals = player.getGoals() != null ? player.getGoals() : 0;
        int matches = player.getMatches() != null ? player.getMatches() : 0;

        if (matches == 0) {
            return PlayerEfficiencyResponse.builder()
                    .playerId(id)
                    .playerName(player.getName())
                    .contributionsPerMatch(0)
                    .build();
        }

        double efficiency = (double) (goals + assists) / matches;
        return PlayerEfficiencyResponse.builder()
                .playerId(id)
                .playerName(player.getName())
                .contributionsPerMatch(efficiency)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlayerResponse> getAllPlayers(int page, int size, String sortBy, String sortDir, String search) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "name";
        }
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (search != null && !search.isBlank()) {
            return playerRepository.findByNameContainingIgnoreCase(search.trim(), pageable)
                    .map(playerMapper::mapPlayerToResponseDto);
        }
        return playerRepository.findAll(pageable)
                .map(playerMapper::mapPlayerToResponseDto);
    }

    @Override
    @Transactional
    public void deletePlayer(UUID id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("This player does not exist: " + id));
        playerRepository.delete(player);
    }

}
