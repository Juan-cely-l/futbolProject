package futbol.api.com.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/futbix/v1/seed")
@RequiredArgsConstructor
public class SeedController {

    private final DataSeeder dataSeeder;

    @PostMapping
    public ResponseEntity<Map<String, Object>> runSeed() {
        DataSeeder.SeedResult result = dataSeeder.runSeed();
        return ResponseEntity.ok(Map.of(
                "status", "complete",
                "teamsCreated", result.teamsCreated(),
                "playersCreated", result.playersCreated(),
                "message", result.message()
        ));
    }
}
