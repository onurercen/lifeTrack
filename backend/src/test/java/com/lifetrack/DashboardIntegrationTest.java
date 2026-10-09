package com.lifetrack;

import com.lifetrack.book.entity.Book;
import com.lifetrack.book.entity.BookStatus;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.media.entity.Media;
import com.lifetrack.media.entity.MediaStatus;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.run.entity.Run;
import com.lifetrack.run.repository.RunRepository;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.lifetrack.support.TestUsers.verifiedUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MediaRepository mediaRepository;

    private User currentUser;

    @BeforeEach
    void setUp() {
        runRepository.deleteAll();
        bookRepository.deleteAll();
        mediaRepository.deleteAll();
        userRepository.deleteAll();
        currentUser = userRepository.save(new User("Test User", "test@example.com", "secret123"));
    }

    @Test
    void dashboard_shouldBeEmptyForNewUser() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(verifiedUser("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runs.totalCount").value(0))
            .andExpect(jsonPath("$.runs.totalDistanceKm").value(0.0))
            .andExpect(jsonPath("$.runs.lastSevenDays.length()").value(7))
            .andExpect(jsonPath("$.books.totalCount").value(0))
            .andExpect(jsonPath("$.books.currentlyReading.length()").value(0))
            .andExpect(jsonPath("$.media.totalCount").value(0));
    }

    @Test
    void dashboard_shouldSummariseOnlyTheCurrentUsersData() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        saveRun(currentUser, 5.0, 30, now);
        saveRun(currentUser, 3.0, 20, now.minusDays(2));
        saveRun(currentUser, 10.0, 60, now.minusDays(10)); // outside the 7-day window
        saveBook(currentUser);

        User other = userRepository.save(new User("Other", "other@example.com", "secret123"));
        saveRun(other, 42.0, 200, now);
        saveBook(other);

        mockMvc.perform(get("/api/dashboard").with(verifiedUser("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runs.totalCount").value(3))
            .andExpect(jsonPath("$.runs.totalDistanceKm").value(18.0))
            .andExpect(jsonPath("$.runs.weekCount").value(2))
            .andExpect(jsonPath("$.runs.weekDistanceKm").value(8.0))
            .andExpect(jsonPath("$.runs.weekDurationMinutes").value(50))
            .andExpect(jsonPath("$.runs.lastSevenDays[6].date").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.runs.lastSevenDays[6].distanceKm").value(5.0))
            .andExpect(jsonPath("$.runs.lastSevenDays[4].distanceKm").value(3.0))
            .andExpect(jsonPath("$.books.totalCount").value(1))
            .andExpect(jsonPath("$.media.totalCount").value(0));
    }

    @Test
    void dashboard_shouldSummariseReadingAndWatching() throws Exception {
        LocalDate today = LocalDate.now();
        saveBook(currentUser, "Dune", BookStatus.READING, today.minusDays(3), null);
        saveBook(currentUser, "Bitti", BookStatus.FINISHED, null, today);
        saveBook(currentUser, "Geçen yıl", BookStatus.FINISHED, null, today.withDayOfYear(1).minusDays(1));
        saveBook(currentUser, "Listede", BookStatus.WANT_TO_READ, null, null);
        saveMedia(currentUser, MediaStatus.COMPLETED, today);
        saveMedia(currentUser, MediaStatus.IN_PROGRESS, null);

        User other = userRepository.save(new User("Other", "other@example.com", "secret123"));
        saveBook(other, "Başkası", BookStatus.READING, today, null);

        mockMvc.perform(get("/api/dashboard").with(verifiedUser("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.books.totalCount").value(4))
            .andExpect(jsonPath("$.books.readingCount").value(1))
            .andExpect(jsonPath("$.books.finishedThisYear").value(1))
            .andExpect(jsonPath("$.books.currentlyReading.length()").value(1))
            .andExpect(jsonPath("$.books.currentlyReading[0].title").value("Dune"))
            .andExpect(jsonPath("$.books.currentlyReading[0].currentPage").value(120))
            .andExpect(jsonPath("$.books.currentlyReading[0].pageCount").value(400))
            .andExpect(jsonPath("$.media.totalCount").value(2))
            .andExpect(jsonPath("$.media.inProgressCount").value(1))
            .andExpect(jsonPath("$.media.completedThisYear").value(1));
    }

    @Test
    void dashboard_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    private void saveRun(User owner, double km, int minutes, LocalDateTime runAt) {
        Run run = new Run();
        run.setDistanceKm(km);
        run.setDurationMinutes(minutes);
        run.setCaloriesBurned(100);
        run.setRunAt(runAt);
        run.setUser(owner);
        runRepository.save(run);
    }

    private void saveBook(User owner) {
        saveBook(owner, "Kitap", BookStatus.WANT_TO_READ, null, null);
    }

    private void saveBook(User owner, String title, BookStatus status, LocalDate startedOn, LocalDate finishedOn) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor("Yazar");
        book.setDescription("Açıklama");
        book.setStatus(status);
        book.setPageCount(400);
        book.setCurrentPage(status == BookStatus.READING ? 120 : null);
        book.setStartedOn(startedOn);
        book.setFinishedOn(finishedOn);
        book.setUser(owner);
        bookRepository.save(book);
    }

    private void saveMedia(User owner, MediaStatus status, LocalDate finishedOn) {
        Media media = new Media();
        media.setTitle("Medya");
        media.setType("Film");
        media.setStatus(status);
        media.setFinishedOn(finishedOn);
        media.setUser(owner);
        mediaRepository.save(media);
    }
}
