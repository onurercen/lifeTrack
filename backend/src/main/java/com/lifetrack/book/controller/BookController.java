package com.lifetrack.book.controller;

import com.lifetrack.book.dto.BookResponse;
import com.lifetrack.book.dto.CreateBookRequest;
import com.lifetrack.book.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/books/health")
    public String health() {
        return "Book service is running";
    }

    @GetMapping("/books/search")
    public ResponseEntity<List<BookResponse>> searchBooks(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(bookService.searchBooks(query, getCurrentUserEmail()));
    }

    @GetMapping("/users/books")
    public ResponseEntity<List<BookResponse>> getUserBooks() {
        return ResponseEntity.ok(bookService.getUserBooks(getCurrentUserEmail()));
    }

    @PostMapping("/user/books")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(bookService.createBook(request, getCurrentUserEmail()));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}
