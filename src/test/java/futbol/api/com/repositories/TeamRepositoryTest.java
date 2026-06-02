package futbol.api.com.repositories;

import futbol.api.com.models.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("TeamRepository integration tests")
class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    private Team barcelona;
    private Team madrid;

    @BeforeEach
    void setUp() {
        barcelona = teamRepository.save(Team.builder()
                .name("Barcelona")
                .budget(500_000_000L)
                .city("Barcelona")
                .build());

        madrid = teamRepository.save(Team.builder()
                .name("Real Madrid")
                .budget(600_000_000L)
                .city("Madrid")
                .build());
    }

    @Test
    @DisplayName("findTeamByNameIgnoreCase should find team by exact name")
    void findTeamByNameIgnoreCase_exactMatch_returnsTeam() {
        Optional<Team> result = teamRepository.findTeamByNameIgnoreCase("Barcelona");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(barcelona.getId());
        assertThat(result.get().getBudget()).isEqualTo(500_000_000L);
    }

    @Test
    @DisplayName("findTeamByNameIgnoreCase should find team with case-insensitive name")
    void findTeamByNameIgnoreCase_caseInsensitive_returnsTeam() {
        Optional<Team> result = teamRepository.findTeamByNameIgnoreCase("barcelona");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(barcelona.getId());
    }

    @Test
    @DisplayName("findTeamByNameIgnoreCase should return empty when team does not exist")
    void findTeamByNameIgnoreCase_notFound_returnsEmpty() {
        Optional<Team> result = teamRepository.findTeamByNameIgnoreCase("NonExistentTeam");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByNameIgnoreCase should return true when team exists")
    void existsByNameIgnoreCase_teamExists_returnsTrue() {
        assertThat(teamRepository.existsByNameIgnoreCase("Real Madrid")).isTrue();
    }

    @Test
    @DisplayName("existsByNameIgnoreCase should return true with case-insensitive match")
    void existsByNameIgnoreCase_caseInsensitive_returnsTrue() {
        assertThat(teamRepository.existsByNameIgnoreCase("real madrid")).isTrue();
    }

    @Test
    @DisplayName("existsByNameIgnoreCase should return false when team does not exist")
    void existsByNameIgnoreCase_teamDoesNotExist_returnsFalse() {
        assertThat(teamRepository.existsByNameIgnoreCase("Atletico Madrid")).isFalse();
    }
}
