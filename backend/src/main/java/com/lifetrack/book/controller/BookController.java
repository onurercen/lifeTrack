package com.lifetrack.book.controller;

import com.lifetrack.book.dto.BookResponse;
import com.lifetrack.book.dto.CreateBookRequest;
import com.lifetrack.book.service.BookService;
import com.lifetrack.book.entity.BookStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> getBooks(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) BookStatus status,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(bookService.searchBooks(query, status, principal.getUsername()));
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(
        @Valid @RequestBody CreateBookRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(bookService.createBook(request, principal.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
        @PathVariable Long id,
        @Valid @RequestBody CreateBookRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(bookService.updateBook(id, request, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        bookService.deleteBook(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
