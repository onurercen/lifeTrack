package com.lifetrack;

import com.lifetrack.book.entity.Book;
import com.lifetrack.book.entity.BookStatus;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.media.entity.MediaStatus;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the application on real PostgreSQL: Flyway migrates and Hibernate's
 * ddl-auto=validate fails the context if entities and migrations disagree.
 * Also runs the queries whose SQL H2 might accept but PostgreSQL wouldn't.
 */
@SpringBootTest
@ActiveProfiles("test")
class PostgresSchemaValidationTest {

    private static final EmbeddedPostgres POSTGRES = start();

    private static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @AfterAll
    static void stop() throws IOException {
        POSTGRES.close();
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Test
    void contextLoads_withMigratedPostgresSchema() {
        // Passing means Flyway ran and Hibernate validated every entity against PostgreSQL.
    }

    @Test
    void searchQueries_shouldRunWithAndWithoutStatusFilter() {
        User user = userRepository.save(new User("PG", "pg@example.com", "secret123"));
        saveBook(user, "Dune", BookStatus.READING);
        saveBook(user, "Clean Code", BookStatus.WANT_TO_READ);

        // A null status and an empty query must bind as typed parameters on PostgreSQL.
        assertThat(bookRepository.search("pg@example.com", "", null)).hasSize(2);
        assertThat(bookRepository.search("pg@example.com", "", BookStatus.READING))
            .extracting(Book::getTitle).containsExactly("Dune");
        assertThat(bookRepository.search("pg@example.com", "CLEAN", null))
            .extracting(Book::getTitle).containsExactly("Clean Code");
        assertThat(mediaRepository.search("pg@example.com", "", null)).isEmpty();
        assertThat(mediaRepository.search("pg@example.com", "x", MediaStatus.COMPLETED)).isEmpty();
    }

    private void saveBook(User owner, String title, BookStatus status) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor("Yazar");
        book.setStatus(status);
        book.setUser(owner);
        bookRepository.save(book);
    }
}
