package com.lifetrack.book.repository;

import com.lifetrack.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByUserEmailOrderByCreatedAtDesc(String email);

    Optional<Book> findByIdAndUserEmail(Long id, String email);

    long countByUserEmail(String email);

    @Query("""
        select b from Book b
        where b.user.email = :email
          and (lower(b.title) like lower(concat('%', :query, '%'))
            or lower(b.author) like lower(concat('%', :query, '%'))
            or lower(b.description) like lower(concat('%', :query, '%')))
        order by b.createdAt desc
        """)
    List<Book> search(@Param("email") String email, @Param("query") String query);
}
