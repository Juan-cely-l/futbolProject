package futbol.api.com.repositories;

import futbol.api.com.models.Player;
import futbol.api.com.models.Position;
import futbol.api.com.models.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("PlayerRepository integration tests")
class PlayerRepositoryTest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    private Team barcelona;
    private Team madrid;
    private Player messi;
    private Player ronaldo;
    private Player pedri;

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

        messi = playerRepository.save(Player.builder()
                .name("Lionel Messi")
                .age(36)
                .goals(30)
                .assists(15)
                .matches(35)
                .position(Position.FORWARD)
                .valueMarket(50_000_000)
                .team(barcelona)
                .build());

        ronaldo = playerRepository.save(Player.builder()
                .name("Cristiano Ronaldo")
                .age(39)
                .goals(25)
                .assists(5)
                .matches(30)
                .position(Position.FORWARD)
                .valueMarket(30_000_000)
                .team(madrid)
                .build());

        pedri = playerRepository.save(Player.builder()
                .name("Pedri")
                .age(21)
                .goals(5)
                .assists(10)
                .matches(28)
                .position(Position.MIDFIELDER)
                .valueMarket(45_000_000)
                .team(barcelona)
                .build());
    }

    @Test
    @DisplayName("findByName should find players with matching name")
    void findByName_returnsMatchingPlayers() {
        List<Player> players = playerRepository.findByName("Lionel Messi");

        assertThat(players).hasSize(1);
        assertThat(players.get(0).getId()).isEqualTo(messi.getId());
    }

    @Test
    @DisplayName("findByName should return empty list when no player matches")
    void findByName_noMatch_returnsEmptyList() {
        assertThat(playerRepository.findByName("Unknown Player")).isEmpty();
    }

    @Test
    @DisplayName("findPlayersByTeam_Name should return players for given team name")
    void findPlayersByTeam_Name_returnsTeamPlayers() {
        List<Player> players = playerRepository.findPlayersByTeam_Name("Barcelona");

        assertThat(players).hasSize(2);
        assertThat(players).extracting(Player::getName)
                .containsExactlyInAnyOrder("Lionel Messi", "Pedri");
    }

    @Test
    @DisplayName("findPlayersByTeam_Name should return empty list for non-existent team name")
    void findPlayersByTeam_Name_noMatch_returnsEmptyList() {
        assertThat(playerRepository.findPlayersByTeam_Name("NonExistentTeam")).isEmpty();
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase should return players with case-insensitive partial match")
    void findByNameContainingIgnoreCase_returnsMatchingPlayers() {
        var result = playerRepository.findByNameContainingIgnoreCase("mess", Pageable.ofSize(10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(messi.getId());
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase should return empty page when no match")
    void findByNameContainingIgnoreCase_noMatch_returnsEmpty() {
        var result = playerRepository.findByNameContainingIgnoreCase("zzzzz", Pageable.ofSize(10));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("sumValueMarketByTeamId should return total market value for team with players")
    void sumValueMarketByTeamId_withPlayers_returnsSum() {
        Long totalValue = playerRepository.sumValueMarketByTeamId(barcelona.getId());

        assertThat(totalValue).isEqualTo(95_000_000L);
    }

    @Test
    @DisplayName("sumValueMarketByTeamId should return null when team has no players")
    void sumValueMarketByTeamId_noPlayers_returnsNull() {
        Team emptyTeam = teamRepository.save(Team.builder()
                .name("Losers FC")
                .budget(50_000_000L)
                .city("Lost City")
                .build());

        assertThat(playerRepository.sumValueMarketByTeamId(emptyTeam.getId())).isNull();
    }

    @Test
    @DisplayName("existsPlayerByNameAndAgeAndTeamName should return true when player exists")
    void existsPlayerByNameAndAgeAndTeamName_exists_returnsTrue() {
        assertThat(playerRepository.existsPlayerByNameAndAgeAndTeamName(
                "Lionel Messi", 36, "Barcelona")).isTrue();
    }

    @Test
    @DisplayName("existsPlayerByNameAndAgeAndTeamName should return false when player does not exist")
    void existsPlayerByNameAndAgeAndTeamName_notExists_returnsFalse() {
        assertThat(playerRepository.existsPlayerByNameAndAgeAndTeamName(
                "Kylian Mbappe", 25, "Barcelona")).isFalse();
    }

    @Test
    @DisplayName("existsPlayerByNameAndAgeAndTeamName should return false when name does not match")
    void existsPlayerByNameAndAgeAndTeamName_wrongName_returnsFalse() {
        assertThat(playerRepository.existsPlayerByNameAndAgeAndTeamName(
                "Wrong Name", 36, "Barcelona")).isFalse();
    }

    @Test
    @DisplayName("existsPlayerByNameAndAgeAndTeamName should return false when age does not match")
    void existsPlayerByNameAndAgeAndTeamName_wrongAge_returnsFalse() {
        assertThat(playerRepository.existsPlayerByNameAndAgeAndTeamName(
                "Lionel Messi", 99, "Barcelona")).isFalse();
    }

    @Test
    @DisplayName("existsPlayerByNameAndAgeAndTeamName should return false when team name does not match")
    void existsPlayerByNameAndAgeAndTeamName_wrongTeam_returnsFalse() {
        assertThat(playerRepository.existsPlayerByNameAndAgeAndTeamName(
                "Lionel Messi", 36, "Real Madrid")).isFalse();
    }
}
