package com.example.lms.users.repository;

import com.example.lms.users.model.LibraryUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibraryUserRepository extends JpaRepository<LibraryUser, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<LibraryUser> findByEmailIgnoreCase(String email);
}
