package futbol.api.com.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "ADMIN_PASSWORD=test123",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
@DisplayName("Flyway migration integration tests")
class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("context starts with Flyway migrations and Hibernate validation")
    void contextStartsWithFlywayMigrationsAndHibernateValidation() {
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");

        assertThat(tableExists("flyway_schema_history")).isTrue();
        assertThat(tableExists("team")).isTrue();
        assertThat(tableExists("player")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where lower(table_name) = ?
                """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }
}
