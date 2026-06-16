package futbol.api.com.services.Team;

import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.team.CreateTeamRequest;
import futbol.api.com.dtos.team.TeamResponse;
import futbol.api.com.dtos.team.TeamValueResponse;
import futbol.api.com.dtos.team.UpdateTeamRequest;
import futbol.api.com.exceptions.ResourceAlreadyExistsException;
import futbol.api.com.exceptions.ResourceNotFoundException;
import futbol.api.com.models.Team;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import futbol.api.com.services.Player.PlayerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "budget", "city", "createdAt"
    );

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> getTeamSquad(String name) {
        if (!teamRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceNotFoundException("This team does not exist: " + name);
        }
        return playerRepository.findPlayersByTeam_Name(name)
                .stream()
                .map(playerMapper::mapPlayerToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamValueResponse getTeamValue(String name) {
        Team team = teamRepository.findTeamByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("This team does not exist: " + name));

        Long teamValue = playerRepository.sumValueMarketByTeamId(team.getId());
        if (teamValue == null) {
            teamValue = 0L;
        }
        return TeamValueResponse.builder()
                .teamName(team.getName())
                .totalValue(teamValue)
                .build();
    }

    @Override
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        String name = request.getName().toLowerCase().trim();
        if (teamRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceAlreadyExistsException("This team already exists" + request.getName());
        }
        Team team = Team.builder()
                .name(name)
                .budget(request.getBudget())
                .city(request.getCity().toLowerCase().trim())
                .build();
        try {
            Team savedTeam = teamRepository.save(team);
            return mapToResponseDto(savedTeam);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("This team already exists" + request.getName());
        }
    }

    @Override
    @Transactional
    public TeamResponse updateTeam(UUID id, UpdateTeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));
        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().toLowerCase().trim();
            if (!team.getName().equals(newName) && teamRepository.existsByNameIgnoreCase(newName)) {
                throw new ResourceAlreadyExistsException("This team already exists: " + newName);
            }
            team.setName(newName);
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            team.setCity(request.getCity().toLowerCase().trim());
        }
        if (request.getBudget() != null) {
            team.setBudget(request.getBudget());
        }
        Team updated = teamRepository.save(team);
        return mapToResponseDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));
        return mapToResponseDto(team);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamByName(String name) {
        Team team = teamRepository.findTeamByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("This team doesn't exist: " + name));
        return mapToResponseDto(team);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamResponse> getAllTeams(int page, int size, String sortBy, String sortDir, String search) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "name";
        }
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (search != null && !search.isBlank()) {
            return teamRepository.findByNameContainingIgnoreCase(search.trim(), pageable)
                    .map(this::mapToResponseDto);
        }
        return teamRepository.findAll(pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    @Transactional
    public void deleteTeam(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
        playerRepository.deleteAllByTeamId(team.getId());
        teamRepository.delete(team);
    }

    private TeamResponse mapToResponseDto(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .budget(team.getBudget())
                .city(team.getCity())
                .squadCount((int) playerRepository.countByTeam_Id(team.getId()))
                .createdAt(team.getCreatedAt())
                .build();
    }
}
