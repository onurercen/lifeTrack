package com.lifetrack.book.service;

import com.lifetrack.book.dto.BookResponse;
import com.lifetrack.book.dto.CreateBookRequest;
import com.lifetrack.book.entity.Book;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BookService(BookRepository bookRepository, UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    public BookResponse createBook(CreateBookRequest request, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ApiException("Kullanıcı bulunamadı"));

        Book book = new Book();
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setDescription(request.getDescription().trim());
        book.setUser(user);

        Book saved = bookRepository.save(book);
        return toResponse(saved);
    }

    public List<BookResponse> getUserBooks(String email) {
        return bookRepository.findByUserEmailOrderByCreatedAtDesc(email)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<BookResponse> searchBooks(String query, String email) {
        String normalized = query == null ? "" : query.trim();
        return bookRepository.findByUserEmailOrderByCreatedAtDesc(email)
            .stream()
            .filter(book -> normalized.isEmpty()
                || book.getTitle().toLowerCase(Locale.ROOT).contains(normalized.toLowerCase(Locale.ROOT))
                || book.getAuthor().toLowerCase(Locale.ROOT).contains(normalized.toLowerCase(Locale.ROOT))
                || book.getDescription().toLowerCase(Locale.ROOT).contains(normalized.toLowerCase(Locale.ROOT)))
            .map(this::toResponse)
            .toList();
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getDescription(),
            book.getUser().getEmail(),
            book.getCreatedAt()
        );
    }
}
