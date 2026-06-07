package futbol.api.com.seed;

import tools.jackson.databind.ObjectMapper;
import futbol.api.com.external.mapper.MarketValueCalculator;
import futbol.api.com.external.mapper.TeamNameNormalizer;
import futbol.api.com.models.Player;
import futbol.api.com.models.Position;
import futbol.api.com.models.Team;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import futbol.api.com.seed.dto.EquipoData;
import futbol.api.com.seed.dto.JugadorStats;
import futbol.api.com.seed.dto.LigaData;
import futbol.api.com.seed.dto.SeedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSeeder {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;
    private final MarketValueCalculator marketValueCalculator;

    @Value("${seed.data.path:seed-data/liga_stats.json}")
    private String seedDataPath;

    private static final Map<String, Double> LIGA_BUDGET_MULTIPLIER = Map.of(
            "ENG_PL", 1.0,
            "ES_LALIGA", 0.75,
            "GER_BL", 0.65,
            "IT_SERIEA", 0.55,
            "FR_LIGUE1", 0.45
    );

    private static final Map<String, Long> LIGA_BASE_BUDGET = Map.of(
            "ENG_PL", 150_000_000L,
            "ES_LALIGA", 80_000_000L,
            "GER_BL", 80_000_000L,
            "IT_SERIEA", 60_000_000L,
            "FR_LIGUE1", 50_000_000L
    );

    private static final Set<String> PREFIXES = Set.of("fc", "real", "club", "ac", "as", "ssc", "sc", "rc", "ogc", "sb");

    @Transactional
    public SeedResult runSeed() {
        SeedData data = loadJson();
        if (data == null) {
            return new SeedResult(0, 0, "Failed to load seed data JSON");
        }

        int teamsCreated = 0;
        int playersCreated = 0;
        int teamsSkipped = 0;
        int playersSkipped = 0;

        // Mapa: equipo_id del JSON → Team entity (para resolver FK de jugadores)
        Map<String, Team> equipoIdToTeam = new HashMap<>();

        for (LigaData liga : data.getLigas()) {
            double mult = LIGA_BUDGET_MULTIPLIER.getOrDefault(liga.getLigaId(), 0.5);
            long baseBudget = LIGA_BASE_BUDGET.getOrDefault(liga.getLigaId(), 50_000_000L);

            // 1. Crear equipos de esta liga
            for (EquipoData eq : liga.getEquipos()) {
                String name = TeamNameNormalizer.normalize(eq.getNombre());
                String city = extractCity(eq.getNombre(), liga.getPais());

                if (teamRepository.existsByNameIgnoreCase(name)) {
                    Team existing = teamRepository.findTeamByNameIgnoreCase(name).orElse(null);
                    if (existing != null) {
                        equipoIdToTeam.put(eq.getEquipoId(), existing);
                    }
                    teamsSkipped++;
                    continue;
                }

                long budget = generateBudget(baseBudget, eq.getPosicionFinal(), mult);

                Team team = Team.builder()
                        .name(name)
                        .city(city)
                        .budget(budget)
                        .build();

                Team saved = teamRepository.save(team);
                equipoIdToTeam.put(eq.getEquipoId(), saved);
                teamsCreated++;
            }

            // 2. Crear jugadores de esta liga
            if (liga.getEstadisticasJugadores() != null) {
                for (JugadorStats js : liga.getEstadisticasJugadores()) {
                    Team team = equipoIdToTeam.get(js.getEquipoId());
                    if (team == null) {
                        log.warn("Equipo {} no encontrado para jugador {}, saltando", js.getEquipoId(), js.getNombre());
                        playersSkipped++;
                        continue;
                    }

                    Position position = mapPosition(js.getPosicion());
                    int age = generateAge(position);
                    String name = js.getNombre().toLowerCase().trim();
                    Integer valueMarket = marketValueCalculator.calculate(
                            position, age,
                            js.getGoles(),
                            js.getAsistencias(),
                            js.getPartidosJugados());

                    if (playerRepository.existsPlayerByNameAndAgeAndTeamName(name, age, team.getName())) {
                        playersSkipped++;
                        continue;
                    }

                    Player player = Player.builder()
                            .name(name)
                            .goals(js.getGoles())
                            .position(position)
                            .age(age)
                            .assists(js.getAsistencias())
                            .matches(js.getPartidosJugados())
                            .valueMarket(valueMarket)
                            .team(team)
                            .build();

                    playerRepository.save(player);
                    playersCreated++;
                }
            }
        }

        String msg = String.format(
                "Seed complete: %d teams created (%d skipped), %d players created (%d skipped)",
                teamsCreated, teamsSkipped, playersCreated, playersSkipped
        );
        log.info(msg);
        return new SeedResult(teamsCreated, playersCreated, msg);
    }

    private SeedData loadJson() {
        try {
            var resource = new ClassPathResource(seedDataPath);
            return objectMapper.readValue(resource.getInputStream(), SeedData.class);
        } catch (IOException e) {
            log.error("Failed to load seed data from {}", seedDataPath, e);
            return null;
        }
    }

    private String extractCity(String teamName, String country) {
        String[] parts = teamName.toLowerCase().split(" ");
        // Si el nombre tiene 2+ palabras, la última suele ser la ciudad (a menos que sea prefijo)
        if (parts.length >= 2) {
            // Quitar prefijos y tomar el resto como ciudad
            List<String> filtered = new ArrayList<>();
            for (String p : parts) {
                if (!PREFIXES.contains(p)) {
                    filtered.add(p);
                }
            }
            if (!filtered.isEmpty()) {
                return filtered.get(filtered.size() - 1);
            }
        }
        // Fallback al país
        return country.toLowerCase().trim();
    }

    private long generateBudget(long baseBudget, int posicion, double ligaMultiplier) {
        // Equipos mejor posicionados tienen más presupuesto
        long positionBonus = Math.max(0, (20 - posicion)) * 10_000_000L;
        long randomFactor = ThreadLocalRandom.current().nextLong(0, 40_000_000L);
        return (long) ((baseBudget + positionBonus + randomFactor) * ligaMultiplier);
    }

    private Position mapPosition(String spanishPosition) {
        return switch (spanishPosition.toLowerCase()) {
            case "portero" -> Position.GOALKEEPER;
            case "defensa", "defensa central", "lateral", "carrilero" -> Position.DEFENDER;
            case "centrocampista", "mediocentro", "pivote", "medio" -> Position.MIDFIELDER;
            case "delantero", "extremo", "ala" -> Position.FORWARD;
            default -> {
                log.warn("Posición desconocida '{}', mapeando a MIDFIELDER", spanishPosition);
                yield Position.MIDFIELDER;
            }
        };
    }

    private int generateAge(Position position) {
        int base;
        if (position == Position.GOALKEEPER) {
            base = ThreadLocalRandom.current().nextInt(23, 36);
        } else if (position == Position.DEFENDER) {
            base = ThreadLocalRandom.current().nextInt(20, 34);
        } else {
            base = ThreadLocalRandom.current().nextInt(18, 33);
        }
        return base;
    }



    public record SeedResult(int teamsCreated, int playersCreated, String message) {}
}
