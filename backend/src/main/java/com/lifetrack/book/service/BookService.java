package com.lifetrack.book.service;

import com.lifetrack.book.dto.BookResponse;
import com.lifetrack.book.dto.CreateBookRequest;
import com.lifetrack.book.entity.Book;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import static com.lifetrack.common.util.Strings.trimToNull;

import java.util.List;

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
            .orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));

        Book book = new Book();
        applyRequest(book, request);
        book.setUser(user);

        Book saved = bookRepository.save(book);
        return toResponse(saved);
    }

    public BookResponse updateBook(Long id, CreateBookRequest request, String email) {
        Book book = findOwnedBook(id, email);
        applyRequest(book, request);
        return toResponse(bookRepository.save(book));
    }

    public void deleteBook(Long id, String email) {
        bookRepository.delete(findOwnedBook(id, email));
    }

    public List<BookResponse> searchBooks(String query, String email) {
        String normalized = query == null ? "" : query.trim();
        List<Book> books = normalized.isEmpty()
            ? bookRepository.findByUserEmailOrderByCreatedAtDesc(email)
            : bookRepository.search(email, normalized);
        return books.stream().map(this::toResponse).toList();
    }

    // Another user's book is reported as missing, so ids can't be probed.
    private Book findOwnedBook(Long id, String email) {
        return bookRepository.findByIdAndUserEmail(id, email)
            .orElseThrow(() -> ApiException.notFound("Kitap bulunamadı"));
    }

    private void applyRequest(Book book, CreateBookRequest request) {
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setDescription(trimToNull(request.getDescription()));
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
