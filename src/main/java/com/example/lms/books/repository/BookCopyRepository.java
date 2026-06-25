package com.example.lms.books.repository;

import com.example.lms.books.model.BookCopy;
import com.example.lms.books.model.BookCopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    long countByTitle(String title);

    long countByTitleAndStatus(String title, BookCopyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bookCopy from BookCopy bookCopy where bookCopy.bookCopyId = :bookCopyId")
    Optional<BookCopy> findByIdForUpdate(@Param("bookCopyId") Long bookCopyId);
}
