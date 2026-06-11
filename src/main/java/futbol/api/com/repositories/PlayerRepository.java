package futbol.api.com.repositories;

import futbol.api.com.models.Player;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    List<Player> findByName(String name);

    List<Player> findByTeamId(UUID teamId);

    List<Player> findPlayersByTeam_Name(String teamName);

    List<Player> findByAge(Integer age);

    @EntityGraph(attributePaths = "team")
    Page<Player> findAll(@NonNull Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<Player> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Player p WHERE p.team.id = :teamId")
    void deleteAllByTeamId(@Param("teamId") UUID teamId);

    @Query("select sum(p.valueMarket) from Player p where p.team.id = :teamId")
    Long sumValueMarketByTeamId(@Param("teamId") UUID teamId);

    boolean existsPlayerByNameAndAgeAndTeamName(String name, Integer age, String teamName);

    long countByTeam_Id(UUID teamId);

    boolean existsByNameAndAgeAndTeamNameAndIdNot(String name, Integer age, String teamName, UUID id);
}
