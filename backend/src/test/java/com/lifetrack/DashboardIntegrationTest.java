package com.lifetrack;

import com.lifetrack.book.entity.Book;
import com.lifetrack.book.repository.BookRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
        mockMvc.perform(get("/api/dashboard").with(user("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runs.totalCount").value(0))
            .andExpect(jsonPath("$.runs.totalDistanceKm").value(0.0))
            .andExpect(jsonPath("$.runs.lastSevenDays.length()").value(7))
            .andExpect(jsonPath("$.bookCount").value(0))
            .andExpect(jsonPath("$.mediaCount").value(0));
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

        mockMvc.perform(get("/api/dashboard").with(user("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runs.totalCount").value(3))
            .andExpect(jsonPath("$.runs.totalDistanceKm").value(18.0))
            .andExpect(jsonPath("$.runs.weekCount").value(2))
            .andExpect(jsonPath("$.runs.weekDistanceKm").value(8.0))
            .andExpect(jsonPath("$.runs.weekDurationMinutes").value(50))
            .andExpect(jsonPath("$.runs.lastSevenDays[6].date").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.runs.lastSevenDays[6].distanceKm").value(5.0))
            .andExpect(jsonPath("$.runs.lastSevenDays[4].distanceKm").value(3.0))
            .andExpect(jsonPath("$.bookCount").value(1))
            .andExpect(jsonPath("$.mediaCount").value(0));
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
        Book book = new Book();
        book.setTitle("Kitap");
        book.setAuthor("Yazar");
        book.setDescription("Açıklama");
        book.setUser(owner);
        bookRepository.save(book);
    }
}
