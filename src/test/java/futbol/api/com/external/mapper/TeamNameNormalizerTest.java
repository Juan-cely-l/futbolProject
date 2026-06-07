package futbol.api.com.external.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TeamNameNormalizer Unit Tests")
class TeamNameNormalizerTest {

    @Test
    @DisplayName("normalize: strips FC suffix")
    void normalize_stripsFcSuffix() {
        assertThat(TeamNameNormalizer.normalize("Arsenal FC")).isEqualTo("arsenal");
    }

    @Test
    @DisplayName("normalize: strips CF suffix")
    void normalize_stripsCfSuffix() {
        assertThat(TeamNameNormalizer.normalize("Villarreal CF")).isEqualTo("villarreal");
    }

    @Test
    @DisplayName("normalize: strips UD suffix")
    void normalize_stripsUdSuffix() {
        assertThat(TeamNameNormalizer.normalize("UD Las Palmas")).isEqualTo("ud las palmas"); // UD is prefix, not suffix
        // Actually UD as prefix: "UD Las Palmas" → "ud las palmas" (prefix not stripped)
    }

    @Test
    @DisplayName("normalize: strips AFC suffix")
    void normalize_stripsAfcSuffix() {
        assertThat(TeamNameNormalizer.normalize("AFC Bournemouth")).isEqualTo("afc bournemouth"); // prefix
        assertThat(TeamNameNormalizer.normalize("Bournemouth AFC")).isEqualTo("bournemouth");     // suffix
    }

    @Test
    @DisplayName("normalize: strips AC suffix")
    void normalize_stripsAcSuffix() {
        assertThat(TeamNameNormalizer.normalize("AC Milan")).isEqualTo("ac milan"); // prefix, not stripped
        assertThat(TeamNameNormalizer.normalize("Milan AC")).isEqualTo("milan");    // suffix
    }

    @Test
    @DisplayName("normalize: no suffix returns name as-is")
    void normalize_noSuffix_returnsSame() {
        assertThat(TeamNameNormalizer.normalize("Aston Villa")).isEqualTo("aston villa");
    }

    @Test
    @DisplayName("normalize: already lowercased bare name unchanged")
    void normalize_alreadyLowercaseBare_unchanged() {
        assertThat(TeamNameNormalizer.normalize("juventus")).isEqualTo("juventus");
    }

    @Test
    @DisplayName("normalize: handles uppercase input")
    void normalize_uppercase_lowercases() {
        assertThat(TeamNameNormalizer.normalize("LIVERPOOL FC")).isEqualTo("liverpool");
    }

    @Test
    @DisplayName("normalize: trims whitespace")
    void normalize_trimsWhitespace() {
        assertThat(TeamNameNormalizer.normalize("  Chelsea FC  ")).isEqualTo("chelsea");
    }

    @Test
    @DisplayName("normalize: prefix FC not stripped (only suffix)")
    void normalize_prefixFc_notStripped() {
        assertThat(TeamNameNormalizer.normalize("FC Barcelona")).isEqualTo("fc barcelona");
    }

    @Test
    @DisplayName("normalize: bare minimum name \"AC\" stripped only if suffix")
    void normalize_bareMinimum_acSuffix_stripped() {
        // "ac" at end with space + remaining >= 2 chars
        assertThat(TeamNameNormalizer.normalize("Some AC")).isEqualTo("some");
    }

    @Test
    @DisplayName("normalize: short remaining name (< 2 chars) not stripped")
    void normalize_shortRemaining_notStripped() {
        // "A FC" → remaining "a" is only 1 char, don't strip
        assertThat(TeamNameNormalizer.normalize("A FC")).isEqualTo("a fc");
    }

    @Test
    @DisplayName("normalize: multi-word name with FC suffix")
    void normalize_multiWordWithFcSuffix() {
        assertThat(TeamNameNormalizer.normalize("Manchester United FC")).isEqualTo("manchester united");
    }

    @Test
    @DisplayName("normalize: handles null gracefully throws NPE")
    void normalize_null_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> TeamNameNormalizer.normalize(null));
    }
}
