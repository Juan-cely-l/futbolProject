package futbol.api.com.external.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Phase D Sync OpenAPI Contract Tests")
class SyncOpenApiContractTest {

    private static final Path CONTRACT = Path.of(
            "docs/api/sync-api.openapi.yaml");

    @Test
    @DisplayName("OpenAPI contract declares start sync accepted and conflict responses")
    void phaseDOpenApiContract_declaresStartSyncAcceptedAndConflictResponses() throws IOException {
        Map<String, Object> doc = loadContract();
        Map<String, Object> postSync = operation(doc, "/futbix/v1/sync", "post");
        Map<String, Object> responses = map(postSync.get("responses"));

        assertThat(responses).containsKeys("202", "409", "503");
    }

    @Test
    @DisplayName("OpenAPI contract declares progress success partial and not found responses")
    void phaseDOpenApiContract_declaresProgressSuccessPartialAndNotFoundResponses() throws IOException {
        Map<String, Object> doc = loadContract();
        Map<String, Object> getProgress = operation(doc, "/futbix/v1/sync/{syncId}", "get");
        Map<String, Object> responses = map(getProgress.get("responses"));

        assertThat(responses).containsKeys("200", "404");
        assertThat(Files.readString(CONTRACT)).contains("status: SUCCESS", "status: PARTIAL");
    }

    @Test
    @DisplayName("OpenAPI contract declares maxTeams request examples")
    void phaseDOpenApiContract_declaresMaxTeamsRequestExamples() throws IOException {
        String yaml = Files.readString(CONTRACT);

        assertThat(yaml).contains("maxTeams: 2", "maxTeams: null");
    }

    @Test
    @DisplayName("OpenAPI contract declares canonical sync contract surface")
    void phaseDOpenApiContract_declaresCanonicalSyncContractSurface() throws IOException {
        Map<String, Object> canonical = loadContract(CONTRACT);
        Map<String, Object> canonicalMarker = map(map(canonical.get("info")).get("x-canonical-contract"));

        assertThat(canonical.get("openapi")).isEqualTo("3.1.0");
        assertThat(canonicalMarker.get("canonical")).isEqualTo(Boolean.TRUE);

        assertThat(map(canonical.get("paths")).keySet())
                .contains("/futbix/v1/sync", "/futbix/v1/sync/{syncId}",
                        "/futbix/v1/sync/leagues", "/futbix/v1/sync/seasons");

        assertThat(responseCodes(canonical, "/futbix/v1/sync", "post")).contains("202", "400", "409");
        assertThat(responseCodes(canonical, "/futbix/v1/sync/{syncId}", "get")).contains("200", "400", "404");

        assertThat(schemaNames(canonical))
                .contains("SyncRequest", "StartSyncResponse", "SyncProgress", "SyncTeamResult", "SyncPlayerResult",
                        "ValidationErrorResponse");
    }

    private static Map<String, Object> loadContract() throws IOException {
        return loadContract(CONTRACT);
    }

    private static Map<String, Object> loadContract(Path contract) throws IOException {
        try (InputStream inputStream = Files.newInputStream(contract)) {
            Object loaded = new Yaml().load(inputStream);
            return map(loaded);
        }
    }

    private static Map<String, Object> operation(Map<String, Object> doc, String path, String method) {
        Map<String, Object> paths = map(doc.get("paths"));
        Map<String, Object> pathItem = map(paths.get(path));
        return map(pathItem.get(method));
    }

    private static java.util.Set<String> responseCodes(Map<String, Object> doc, String path, String method) {
        return map(operation(doc, path, method).get("responses")).keySet();
    }

    private static java.util.Set<String> schemaNames(Map<String, Object> doc) {
        return map(map(doc.get("components")).get("schemas")).keySet();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
