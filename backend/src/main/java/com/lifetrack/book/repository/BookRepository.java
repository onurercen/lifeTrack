package com.lifetrack.book.repository;

import com.lifetrack.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByUserEmailOrderByCreatedAtDesc(String email);
}
