package com.lifetrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.run.repository.RunRepository;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static com.lifetrack.support.TestUsers.verifiedUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookAndMediaProgressIntegrationTest {

    private static final String EMAIL = "test@example.com";

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

    @BeforeEach
    void setUp() {
        runRepository.deleteAll();
        bookRepository.deleteAll();
        mediaRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new User("Test User", EMAIL, "secret123"));
    }

    @Test
    void createBook_shouldDefaultToWantToRead() throws Exception {
        send(post("/api/books"), """
            {"title": "Dune", "author": "Frank Herbert"}
            """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("WANT_TO_READ"))
            .andExpect(jsonPath("$.startedOn").doesNotExist());
    }

    @Test
    void book_shouldStampDatesWhenStatusChanges() throws Exception {
        String today = LocalDate.now().toString();
        Long id = idOf(send(post("/api/books"), """
            {"title": "Dune", "author": "Frank Herbert", "status": "READING", "pageCount": 400, "currentPage": 50}
            """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startedOn").value(today))
            .andExpect(jsonPath("$.currentPage").value(50)));

        send(put("/api/books/{id}", id), """
            {"title": "Dune", "author": "Frank Herbert", "status": "FINISHED", "pageCount": 400,
             "currentPage": 50, "startedOn": "%s", "rating": 5}
            """.formatted(today))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.finishedOn").value(today))
            .andExpect(jsonPath("$.currentPage").value(400))
            .andExpect(jsonPath("$.rating").value(5));

        // Back to the reading list: progress and dates are cleared, the rating stays.
        send(put("/api/books/{id}", id), """
            {"title": "Dune", "author": "Frank Herbert", "status": "WANT_TO_READ", "pageCount": 400,
             "currentPage": 400, "startedOn": "%s", "finishedOn": "%s", "rating": 5}
            """.formatted(today, today))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startedOn").doesNotExist())
            .andExpect(jsonPath("$.finishedOn").doesNotExist())
            .andExpect(jsonPath("$.currentPage").doesNotExist())
            .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void updateBook_shouldKeepStatusWhenOmittedAndNotRestampDates() throws Exception {
        Long id = idOf(send(post("/api/books"), """
            {"title": "Dune", "author": "Frank Herbert", "status": "FINISHED"}
            """));

        // An old client that doesn't know about status, and a cleared finish date.
        send(put("/api/books/{id}", id), """
            {"title": "Dune Messiah", "author": "Frank Herbert"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FINISHED"))
            .andExpect(jsonPath("$.finishedOn").doesNotExist());
    }

    @Test
    void book_shouldRejectInconsistentProgress() throws Exception {
        send(post("/api/books"), """
            {"title": "X", "author": "Y", "status": "READING", "pageCount": 100, "currentPage": 150}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Okunan sayfa, sayfa sayısından büyük olamaz"));

        send(post("/api/books"), """
            {"title": "X", "author": "Y", "status": "FINISHED", "startedOn": "2026-05-10", "finishedOn": "2026-05-01"}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Bitiş tarihi başlangıç tarihinden önce olamaz"));

        send(post("/api/books"), """
            {"title": "X", "author": "Y", "status": "READING", "startedOn": "2999-01-01"}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Tarih gelecekte olamaz"));

        send(post("/api/books"), """
            {"title": "X", "author": "Y", "rating": 6}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.rating").value("Puan 1 ile 5 arasında olmalıdır"));

        send(post("/api/books"), """
            {"title": "X", "author": "Y", "status": "SOMETHING"}
            """)
            .andExpect(status().isBadRequest());
    }

    @Test
    void listBooks_shouldFilterByStatusAndQuery() throws Exception {
        send(post("/api/books"), """
            {"title": "Dune", "author": "Frank Herbert", "status": "READING"}
            """);
        send(post("/api/books"), """
            {"title": "Dune Messiah", "author": "Frank Herbert"}
            """);
        send(post("/api/books"), """
            {"title": "Clean Code", "author": "Robert C. Martin", "status": "READING"}
            """);

        mockMvc.perform(get("/api/books").param("status", "READING").with(verifiedUser(EMAIL)))
            .andExpect(jsonPath("$.items.length()").value(2));
        mockMvc.perform(get("/api/books").param("status", "READING").param("query", "herbert").with(verifiedUser(EMAIL)))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].title").value("Dune"));
        mockMvc.perform(get("/api/books").param("status", "NOPE").with(verifiedUser(EMAIL)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void media_shouldTrackStatusRatingAndCompletionDate() throws Exception {
        Long id = idOf(send(post("/api/media"), """
            {"title": "Severance", "type": "Dizi"}
            """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PLANNED"))
            .andExpect(jsonPath("$.finishedOn").doesNotExist()));

        send(put("/api/media/{id}", id), """
            {"title": "Severance", "type": "Dizi", "status": "COMPLETED", "rating": 4}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.finishedOn").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.rating").value(4));

        send(put("/api/media/{id}", id), """
            {"title": "Severance", "type": "Dizi", "status": "IN_PROGRESS", "finishedOn": "2026-01-01"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.finishedOn").doesNotExist());

        mockMvc.perform(get("/api/media").param("status", "IN_PROGRESS").with(verifiedUser(EMAIL)))
            .andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(get("/api/media").param("status", "COMPLETED").with(verifiedUser(EMAIL)))
            .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void media_shouldRejectFutureCompletionDate() throws Exception {
        send(post("/api/media"), """
            {"title": "X", "type": "Film", "status": "COMPLETED", "finishedOn": "2999-01-01"}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Tarih gelecekte olamaz"));
    }

    private ResultActions send(
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
        String body
    ) throws Exception {
        return mockMvc.perform(request.with(verifiedUser(EMAIL)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
