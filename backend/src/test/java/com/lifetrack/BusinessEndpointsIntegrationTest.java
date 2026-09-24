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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

        mockMvc.perform(post("/api/user/books")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Clean Code"));

        mockMvc.perform(get("/api/books/search").param("query", "clean").with(user("test@example.com")))
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

        mockMvc.perform(post("/api/user/media")
                .with(user("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Interstellar"));

        mockMvc.perform(get("/api/media/search").param("query", "interstellar").with(user("test@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Interstellar"));
    }
}
