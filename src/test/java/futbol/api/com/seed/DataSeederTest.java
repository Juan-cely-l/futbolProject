package futbol.api.com.seed;

import futbol.api.com.models.Player;
import futbol.api.com.models.Position;
import futbol.api.com.models.Team;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "seed.data.path=seed-data/test_liga.json")
@DisplayName("DataSeeder integration tests")
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    @DisplayName("runSeed creates the correct number of teams and players")
    void runSeed_createsCorrectNumberOfTeamsAndPlayers() {
        DataSeeder.SeedResult result = dataSeeder.runSeed();

        assertThat(result.teamsCreated()).isEqualTo(2);
        assertThat(result.playersCreated()).isEqualTo(2);

        assertThat(teamRepository.count()).isEqualTo(2);
        assertThat(playerRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("runSeed is idempotent for teams - second call skips existing teams")
    void runSeed_isIdempotentForTeams() {
        dataSeeder.runSeed();

        DataSeeder.SeedResult secondResult = dataSeeder.runSeed();

        // Teams are idempotent because they are looked up by normalized name.
        // Players are NOT fully idempotent because ages are randomly generated
        // each call, so the (name, age, team_name) unique constraint never matches.
        assertThat(secondResult.teamsCreated()).isZero();

        // Verify the DB still has exactly 2 teams (no duplicates)
        assertThat(teamRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Unknown position 'libero' maps to MIDFIELDER with a warning")
    void unknownPosition_mapsToMidfielder() {
        dataSeeder.runSeed();

        List<Player> carlosPlayers = playerRepository.findByName("carlos lopez");
        assertThat(carlosPlayers).hasSize(1);

        Player carlos = carlosPlayers.get(0);
        assertThat(carlos.getPosition()).isEqualTo(Position.MIDFIELDER);
    }

    @Test
    @DisplayName("Known position 'portero' maps to GOALKEEPER")
    void knownPosition_portero_mapsToGoalkeeper() {
        dataSeeder.runSeed();

        List<Player> juanPlayers = playerRepository.findByName("juan perez");
        assertThat(juanPlayers).hasSize(1);

        Player juan = juanPlayers.get(0);
        assertThat(juan.getPosition()).isEqualTo(Position.GOALKEEPER);
    }

    @Test
    @DisplayName("Created teams are queryable by name after seeding")
    void createdTeams_areQueryableByName() {
        dataSeeder.runSeed();

        Team team1 = teamRepository.findTeamByNameIgnoreCase("fc testcity")
                .orElseThrow(() -> new AssertionError("Team 'fc testcity' not found"));
        assertThat(team1.getName()).isEqualTo("fc testcity");

        Team team2 = teamRepository.findTeamByNameIgnoreCase("real testtown")
                .orElseThrow(() -> new AssertionError("Team 'real testtown' not found"));
        assertThat(team2.getName()).isEqualTo("real testtown");
    }

    @Test
    @DisplayName("Created players are queryable by name after seeding")
    void createdPlayers_areQueryableByName() {
        dataSeeder.runSeed();

        List<Player> juan = playerRepository.findByName("juan perez");
        assertThat(juan).hasSize(1);
        assertThat(juan.get(0).getName()).isEqualTo("juan perez");
        assertThat(juan.get(0).getGoals()).isZero();
        assertThat(juan.get(0).getAssists()).isEqualTo(1);
        assertThat(juan.get(0).getMatches()).isEqualTo(20);

        List<Player> carlos = playerRepository.findByName("carlos lopez");
        assertThat(carlos).hasSize(1);
        assertThat(carlos.get(0).getName()).isEqualTo("carlos lopez");
        assertThat(carlos.get(0).getGoals()).isEqualTo(5);
        assertThat(carlos.get(0).getAssists()).isEqualTo(3);
        assertThat(carlos.get(0).getMatches()).isEqualTo(15);
    }

    @Test
    @DisplayName("Team name is lowercased via TeamNameNormalizer - FC and Real suffixes are stripped at end only")
    void teamName_isLowercasedByNormalizer() {
        dataSeeder.runSeed();

        assertThat(teamRepository.existsByNameIgnoreCase("fc testcity")).isTrue();
        assertThat(teamRepository.existsByNameIgnoreCase("real testtown")).isTrue();

        Team team1 = teamRepository.findTeamByNameIgnoreCase("fc testcity")
                .orElseThrow();
        // "FC TestCity" -> lowercase "fc testcity" -> does NOT end with " fc" -> "fc testcity"
        assertThat(team1.getName()).isEqualTo("fc testcity");

        Team team2 = teamRepository.findTeamByNameIgnoreCase("real testtown")
                .orElseThrow();
        assertThat(team2.getName()).isEqualTo("real testtown");
    }

    @Test
    @DisplayName("City is extracted from team name by removing prefixes")
    void city_isExtractedFromTeamName() {
        dataSeeder.runSeed();

        Team team1 = teamRepository.findTeamByNameIgnoreCase("fc testcity")
                .orElseThrow();
        // "FC TestCity": prefixes "fc" stripped -> remaining "testcity"
        assertThat(team1.getCity()).isEqualTo("testcity");

        Team team2 = teamRepository.findTeamByNameIgnoreCase("real testtown")
                .orElseThrow();
        // "Real TestTown": prefixes "real" stripped -> remaining "testtown"
        assertThat(team2.getCity()).isEqualTo("testtown");
    }

    @Test
    @DisplayName("Budget is a positive number computed from position and league multiplier")
    void budget_isPositive() {
        dataSeeder.runSeed();

        List<Team> allTeams = teamRepository.findAll();
        assertThat(allTeams).hasSize(2);
        assertThat(allTeams).allMatch(t -> t.getBudget() > 0);
    }

    @Test
    @DisplayName("Player ages are generated within expected range per position")
    void playerAges_areWithinExpectedRange() {
        dataSeeder.runSeed();

        // Juan Perez is GOALKEEPER -> age range 23-35 (ThreadLocalRandom.nextInt(23, 36))
        List<Player> juan = playerRepository.findByName("juan perez");
        assertThat(juan.get(0).getAge()).isBetween(23, 35);

        // Carlos Lopez is MIDFIELDER -> age range 18-32 (ThreadLocalRandom.nextInt(18, 33))
        List<Player> carlos = playerRepository.findByName("carlos lopez");
        assertThat(carlos.get(0).getAge()).isBetween(18, 32);
    }

    @Test
    @DisplayName("Player market value is computed by MarketValueCalculator")
    void playerMarketValue_isComputed() {
        dataSeeder.runSeed();

        List<Player> juan = playerRepository.findByName("juan perez");
        assertThat(juan.get(0).getValueMarket()).isPositive();

        List<Player> carlos = playerRepository.findByName("carlos lopez");
        assertThat(carlos.get(0).getValueMarket()).isPositive();
    }

    @Test
    @DisplayName("Players reference their correct team")
    void players_referenceCorrectTeam() {
        dataSeeder.runSeed();

        Team team1 = teamRepository.findTeamByNameIgnoreCase("fc testcity")
                .orElseThrow();
        Team team2 = teamRepository.findTeamByNameIgnoreCase("real testtown")
                .orElseThrow();

        List<Player> juan = playerRepository.findByName("juan perez");
        assertThat(juan.get(0).getTeam().getId()).isEqualTo(team1.getId());

        List<Player> carlos = playerRepository.findByName("carlos lopez");
        assertThat(carlos.get(0).getTeam().getId()).isEqualTo(team2.getId());
    }

    @Test
    @DisplayName("Seed result message contains the created and skipped counts")
    void seedResult_containsCountsInMessage() {
        DataSeeder.SeedResult firstResult = dataSeeder.runSeed();

        String msg = firstResult.message();
        assertThat(msg)
                .contains("2 teams created")
                .contains("2 players created");

        // Verify skipped counts are present (format: "X teams created (Y skipped)")
        assertThat(msg).contains("0 skipped");

        DataSeeder.SeedResult secondResult = dataSeeder.runSeed();

        // Second call: teams are skipped (idempotent by name), but players are
        // created again because ages are randomly generated each run
        assertThat(secondResult.message())
                .contains("0 teams created (2 skipped)")
                .contains("players created")
                .containsPattern("\\d+ skipped\\)");
    }
}
