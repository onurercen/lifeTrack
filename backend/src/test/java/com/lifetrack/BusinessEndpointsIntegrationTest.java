package com.lifetrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifetrack.auth.dto.LoginRequest;
import com.lifetrack.book.dto.CreateBookRequest;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.media.dto.CreateMediaRequest;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.run.dto.CreateRunRequest;
import com.lifetrack.run.repository.RunRepository;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    @WithMockUser(username = "test@example.com")
    void runsEndpoints_shouldCreateAndListRuns() throws Exception {
        CreateRunRequest request = new CreateRunRequest();
        request.setDistanceKm(5.2);
        request.setDurationMinutes(30);
        request.setCaloriesBurned(420);
        request.setNotes("Sabah koşusu");

        mockMvc.perform(post("/api/runs")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.distanceKm").value(5.2))
            .andExpect(jsonPath("$.userEmail").value("test@example.com"));

        mockMvc.perform(get("/api/runs").with(user("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userEmail").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void booksEndpoints_shouldCreateAndSearchBooks() throws Exception {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("Clean Code");
        request.setAuthor("Robert C. Martin");
        request.setDescription("Temiz kod yazmak");

        mockMvc.perform(post("/api/books")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Clean Code"));

        mockMvc.perform(get("/api/books").param("query", "clean").with(user("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void mediaEndpoints_shouldCreateAndSearchMedia() throws Exception {
        CreateMediaRequest request = new CreateMediaRequest();
        request.setTitle("Interstellar");
        request.setType("movie");
        request.setUrl("https://example.com/interstellar");
        request.setDescription("Uzay yolculuğu");

        mockMvc.perform(post("/api/media")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Interstellar"));

        mockMvc.perform(get("/api/media").param("query", "interstellar").with(user("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Interstellar"));
    }

    @Test
    void currentUser_shouldReturnProfile() throws Exception {
        mockMvc.perform(get("/api/users/me").with(user("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(currentUser.getId()))
            .andExpect(jsonPath("$.name").value("Test User"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void protectedEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/runs"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldReturn401ForMalformedToken() throws Exception {
        mockMvc.perform(get("/api/runs").header("Authorization", "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createRun_shouldReturnValidationErrors() throws Exception {
        mockMvc.perform(post("/api/runs")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.errors.distanceKm").exists());
    }

    @Test
    void runsEndpoints_shouldUpdateAndDeleteOwnRun() throws Exception {
        Long id = createRun("test@example.com", 5.0);

        CreateRunRequest update = runRequest(10.0);
        update.setNotes("Uzun koşu");
        mockMvc.perform(put("/api/runs/{id}", id)
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.distanceKm").value(10.0))
            .andExpect(jsonPath("$.notes").value("Uzun koşu"));

        mockMvc.perform(delete("/api/runs/{id}", id).with(user("test@example.com")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/runs").with(user("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void runsEndpoints_shouldNotExposeOtherUsersRuns() throws Exception {
        userRepository.save(new User("Other", "other@example.com", "secret123"));
        Long id = createRun("other@example.com", 7.0);

        mockMvc.perform(put("/api/runs/{id}", id)
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(runRequest(1.0))))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/runs/{id}", id).with(user("test@example.com")))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/runs").with(user("test@example.com")))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void booksEndpoints_shouldFilterUpdateAndDelete() throws Exception {
        Long cleanCode = createBook("test@example.com", "Clean Code", "Robert C. Martin");
        createBook("test@example.com", "Dune", "Frank Herbert");

        mockMvc.perform(get("/api/books").with(user("test@example.com")))
            .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/books").param("query", "HERBERT").with(user("test@example.com")))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Dune"));

        mockMvc.perform(put("/api/books/{id}", cleanCode)
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest("Clean Architecture", "Robert C. Martin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Clean Architecture"));

        mockMvc.perform(delete("/api/books/{id}", cleanCode).with(user("test@example.com")))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/books").with(user("test@example.com")))
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void booksEndpoints_shouldNotExposeOtherUsersBooks() throws Exception {
        userRepository.save(new User("Other", "other@example.com", "secret123"));
        Long id = createBook("other@example.com", "Gizli", "Yazar");

        mockMvc.perform(delete("/api/books/{id}", id).with(user("test@example.com")))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/books").with(user("test@example.com")))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void mediaEndpoints_shouldUpdateAndDelete() throws Exception {
        String body = mockMvc.perform(post("/api/media")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mediaRequest("Interstellar"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(put("/api/media/{id}", id)
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mediaRequest("Inception"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Inception"));

        mockMvc.perform(delete("/api/media/{id}", id).with(user("test@example.com")))
            .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/media/{id}", id).with(user("test@example.com")))
            .andExpect(status().isNotFound());
    }

    @Test
    void createRun_shouldAcceptPastRunAtAndMissingCalories() throws Exception {
        mockMvc.perform(post("/api/runs")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"distanceKm": 4.0, "durationMinutes": 25, "runAt": "2026-09-30T18:15:00"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.runAt").value("2026-09-30T18:15:00"))
            .andExpect(jsonPath("$.caloriesBurned").doesNotExist());
    }

    @Test
    void createRun_shouldDefaultRunAtToNowAndSortByIt() throws Exception {
        createRun("test@example.com", 3.0);
        mockMvc.perform(post("/api/runs")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"distanceKm": 9.0, "durationMinutes": 50, "runAt": "2020-01-01T08:00:00"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/runs").with(user("test@example.com")))
            .andExpect(jsonPath("$[0].distanceKm").value(3.0))
            .andExpect(jsonPath("$[0].runAt").exists())
            .andExpect(jsonPath("$[1].distanceKm").value(9.0));
    }

    @Test
    void createRun_shouldRejectFutureRunAt() throws Exception {
        mockMvc.perform(post("/api/runs")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"distanceKm": 4.0, "durationMinutes": 25, "runAt": "2999-01-01T00:00:00"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Koşu tarihi gelecekte olamaz"));
    }

    @Test
    void createMedia_shouldAllowMissingUrlAndDescription() throws Exception {
        mockMvc.perform(post("/api/media")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Severance", "type": "Dizi", "url": "  ", "description": ""}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.url").doesNotExist())
            .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void createMedia_shouldRejectInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/media")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "X", "type": "Film", "url": "not a url"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.url").value("Geçerli bir bağlantı giriniz"));
    }

    @Test
    void createBook_shouldAllowMissingDescription() throws Exception {
        mockMvc.perform(post("/api/books")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Dune", "author": "Frank Herbert"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.description").doesNotExist());
    }

    private CreateBookRequest bookRequest(String title, String author) {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle(title);
        request.setAuthor(author);
        request.setDescription("Açıklama");
        return request;
    }

    private Long createBook(String email, String title, String author) throws Exception {
        String body = mockMvc.perform(post("/api/books")
                .with(user(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest(title, author))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private CreateMediaRequest mediaRequest(String title) {
        CreateMediaRequest request = new CreateMediaRequest();
        request.setTitle(title);
        request.setType("Film");
        request.setUrl("https://example.com/" + title.toLowerCase());
        request.setDescription("Açıklama");
        return request;
    }

    private CreateRunRequest runRequest(double distanceKm) {
        CreateRunRequest request = new CreateRunRequest();
        request.setDistanceKm(distanceKm);
        request.setDurationMinutes(30);
        request.setCaloriesBurned(300);
        return request;
    }

    private Long createRun(String email, double distanceKm) throws Exception {
        String body = mockMvc.perform(post("/api/runs")
                .with(user(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(runRequest(distanceKm))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
