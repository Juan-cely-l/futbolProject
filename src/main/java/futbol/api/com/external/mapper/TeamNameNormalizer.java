package futbol.api.com.external.mapper;

import java.util.Set;

public final class TeamNameNormalizer {

    private static final Set<String> SUFFIXES = Set.of("fc", "cf", "ud", "afc", "ac");

    private TeamNameNormalizer() {}

    public static String normalize(String raw) {
        String name = raw.toLowerCase().trim();

        // Only strip suffix if there's enough left to be meaningful
        for (String suffix : SUFFIXES) {
            if (name.endsWith(" " + suffix)) {
                String stripped = name.substring(0, name.length() - suffix.length() - 1).trim();
                if (stripped.length() >= 2) {
                    return stripped;
                }
            }
        }

        return name;
    }
}
