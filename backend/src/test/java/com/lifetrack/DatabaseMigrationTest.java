package com.lifetrack;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the Flyway migrations against a real PostgreSQL (embedded, no Docker),
 * covering both a fresh database and one created earlier by Hibernate's ddl-auto.
 */
class DatabaseMigrationTest {

    private static EmbeddedPostgres postgres;

    @BeforeAll
    static void startPostgres() throws IOException {
        postgres = EmbeddedPostgres.start();
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        postgres.close();
    }

    @Test
    void freshDatabase_shouldApplyAllMigrations() throws Exception {
        DataSource dataSource = newDatabase("fresh");

        flyway(dataSource).migrate();

        List<String> applied = Arrays.stream(flyway(dataSource).info().applied())
            .map(MigrationInfo::getScript)
            .toList();
        assertThat(applied).contains(
            "V1__initial_schema.sql", "V2__run_date_and_optional_fields.sql", "V3__lowercase_emails.sql",
            "V4__book_and_media_progress.sql");

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(isNullable(jdbc, "runs", "calories_burned")).isTrue();
        assertThat(isNullable(jdbc, "runs", "run_at")).isFalse();
        assertThat(isNullable(jdbc, "media", "url")).isTrue();
        assertThat(isNullable(jdbc, "books", "description")).isTrue();
    }

    @Test
    void legacyHibernateDatabase_shouldBeBaselinedAndKeepData() throws Exception {
        DataSource dataSource = newDatabase("legacy");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // Simulate a database created by ddl-auto=update before Flyway existed.
        jdbc.execute(readMigration("V1__initial_schema.sql"));
        jdbc.update("insert into users (name, email, password, created_at) values ('A', 'a@test.com', 'x', now())");
        Timestamp recordedAt = Timestamp.valueOf("2026-09-01 07:30:00");
        jdbc.update("""
            insert into runs (distance_km, duration_minutes, calories_burned, notes, user_id, created_at)
            values (5.0, 30, 300, 'eski koşu', (select id from users), ?)
            """, recordedAt);

        flyway(dataSource).migrate();

        MigrationInfo[] applied = flyway(dataSource).info().applied();
        assertThat(applied[0].getType().isBaseline()).isTrue();
        assertThat(applied[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(applied).extracting(MigrationInfo::getScript).contains("V2__run_date_and_optional_fields.sql");

        Map<String, Object> run = jdbc.queryForMap("select run_at, created_at, notes from runs");
        assertThat(run.get("run_at")).isEqualTo(recordedAt);
        assertThat(run.get("notes")).isEqualTo("eski koşu");
    }

    @Test
    void legacyDatabase_shouldLowercaseEmailsWithoutBreakingOnCaseDuplicates() throws Exception {
        DataSource dataSource = newDatabase("emails");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(readMigration("V1__initial_schema.sql"));
        String insert = "insert into users (name, email, password, created_at) values ('u', ?, 'x', now())";
        jdbc.update(insert, "Mixed@Case.com");
        jdbc.update(insert, "Dup@Test.com");
        jdbc.update(insert, "dup@test.com");

        flyway(dataSource).migrate();

        List<String> emails = jdbc.queryForList("select email from users order by id", String.class);
        assertThat(emails).containsExactly("mixed@case.com", "Dup@Test.com", "dup@test.com");
    }

    @Test
    void legacyDatabase_shouldGiveExistingBooksAndMediaAStatus() throws Exception {
        DataSource dataSource = newDatabase("progress");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(readMigration("V1__initial_schema.sql"));
        jdbc.update("insert into users (name, email, password, created_at) values ('A', 'a@test.com', 'x', now())");
        jdbc.update("""
            insert into books (title, author, description, user_id, created_at)
            values ('Dune', 'Frank Herbert', 'x', (select id from users), now())
            """);
        jdbc.update("""
            insert into media (title, type, url, description, user_id, created_at)
            values ('Interstellar', 'Film', 'https://e.com', 'x', (select id from users), now())
            """);

        flyway(dataSource).migrate();

        assertThat(jdbc.queryForObject("select status from books", String.class)).isEqualTo("WANT_TO_READ");
        assertThat(jdbc.queryForObject("select status from media", String.class)).isEqualTo("COMPLETED");
        assertThat(isNullable(jdbc, "books", "page_count")).isTrue();
        assertThat(isNullable(jdbc, "media", "finished_on")).isTrue();
    }

    private static Flyway flyway(DataSource dataSource) {
        // Same settings as application.yml.
        return Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .load();
    }

    private static DataSource newDatabase(String name) throws Exception {
        try (var connection = postgres.getPostgresDatabase().getConnection()) {
            connection.createStatement().execute("create database " + name);
        }
        return postgres.getDatabase("postgres", name);
    }

    private static boolean isNullable(JdbcTemplate jdbc, String table, String column) {
        String nullable = jdbc.queryForObject(
            "select is_nullable from information_schema.columns where table_name = ? and column_name = ?",
            String.class, table, column);
        return "YES".equals(nullable);
    }

    private static String readMigration(String name) throws IOException {
        return StreamUtils.copyToString(
            new ClassPathResource("db/migration/" + name).getInputStream(), StandardCharsets.UTF_8);
    }
}
