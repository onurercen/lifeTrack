package com.lifetrack.book.service;

import com.lifetrack.book.dto.BookResponse;
import com.lifetrack.book.dto.CreateBookRequest;
import com.lifetrack.book.entity.Book;
import com.lifetrack.book.entity.BookStatus;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static com.lifetrack.common.util.Strings.trimToNull;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public BookService(BookRepository bookRepository, UserRepository userRepository, Clock clock) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public BookResponse createBook(CreateBookRequest request, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));

        Book book = new Book();
        applyRequest(book, request, null);
        book.setUser(user);

        Book saved = bookRepository.save(book);
        return toResponse(saved);
    }

    public BookResponse updateBook(Long id, CreateBookRequest request, String email) {
        Book book = findOwnedBook(id, email);
        applyRequest(book, request, book.getStatus());
        return toResponse(bookRepository.save(book));
    }

    public void deleteBook(Long id, String email) {
        bookRepository.delete(findOwnedBook(id, email));
    }

    public List<BookResponse> searchBooks(String query, BookStatus status, String email) {
        String normalized = query == null ? "" : query.trim();
        return bookRepository.search(email, normalized, status).stream().map(this::toResponse).toList();
    }

    // Another user's book is reported as missing, so ids can't be probed.
    private Book findOwnedBook(Long id, String email) {
        return bookRepository.findByIdAndUserEmail(id, email)
            .orElseThrow(() -> ApiException.notFound("Kitap bulunamadı"));
    }

    /** [previousStatus] is null for a new book. */
    private void applyRequest(Book book, CreateBookRequest request, BookStatus previousStatus) {
        BookStatus status = request.getStatus() != null ? request.getStatus() : book.getStatus();
        LocalDate today = LocalDate.now(clock);
        LocalDate startedOn = request.getStartedOn();
        LocalDate finishedOn = request.getFinishedOn();
        Integer currentPage = request.getCurrentPage();

        // Dates are filled in only when the status changes, so editing an old
        // record without dates doesn't stamp it with today.
        boolean statusChanged = status != previousStatus;
        switch (status) {
            case WANT_TO_READ -> {
                startedOn = null;
                finishedOn = null;
                currentPage = null;
            }
            case READING -> {
                finishedOn = null;
                if (startedOn == null && statusChanged) {
                    startedOn = today;
                }
            }
            case FINISHED -> {
                if (finishedOn == null && statusChanged) {
                    finishedOn = today;
                }
                if (request.getPageCount() != null) {
                    currentPage = request.getPageCount();
                }
            }
        }

        if (currentPage != null && request.getPageCount() != null && currentPage > request.getPageCount()) {
            throw ApiException.badRequest("Okunan sayfa, sayfa sayısından büyük olamaz");
        }
        if ((startedOn != null && startedOn.isAfter(today)) || (finishedOn != null && finishedOn.isAfter(today))) {
            throw ApiException.badRequest("Tarih gelecekte olamaz");
        }
        if (startedOn != null && finishedOn != null && finishedOn.isBefore(startedOn)) {
            throw ApiException.badRequest("Bitiş tarihi başlangıç tarihinden önce olamaz");
        }

        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setDescription(trimToNull(request.getDescription()));
        book.setStatus(status);
        book.setPageCount(request.getPageCount());
        book.setCurrentPage(currentPage);
        book.setRating(request.getRating());
        book.setStartedOn(startedOn);
        book.setFinishedOn(finishedOn);
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getDescription(),
            book.getStatus(),
            book.getPageCount(),
            book.getCurrentPage(),
            book.getRating(),
            book.getStartedOn(),
            book.getFinishedOn(),
            book.getUser().getEmail(),
            book.getCreatedAt()
        );
    }
}
