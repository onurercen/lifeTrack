package com.lifetrack.book.repository;

import com.lifetrack.book.entity.Book;
import com.lifetrack.book.entity.BookStatus;
import com.lifetrack.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIdAndUserEmail(Long id, String email);

    long countByUserEmail(String email);

    long countByUserEmailAndStatus(String email, BookStatus status);

    long countByUserEmailAndStatusAndFinishedOnGreaterThanEqual(String email, BookStatus status, LocalDate from);

    List<Book> findTop5ByUserEmailAndStatusOrderByStartedOnDescIdDesc(String email, BookStatus status);

    /** An empty [query] matches every book; a null [status] matches every status. */
    @Query("""
        select b from Book b
        where b.user.email = :email
          and (:status is null or b.status = :status)
          and (lower(b.title) like lower(concat('%', :query, '%'))
            or lower(b.author) like lower(concat('%', :query, '%'))
            or lower(b.description) like lower(concat('%', :query, '%')))
        order by b.createdAt desc, b.id desc
        """)
    Page<Book> search(@Param("email") String email, @Param("query") String query, @Param("status") BookStatus status, Pageable pageable);

    @Modifying
    @Query("delete from Book b where b.user = :user")
    int deleteAllByOwner(@Param("user") User user);
}
