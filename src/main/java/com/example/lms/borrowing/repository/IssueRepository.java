package com.example.lms.borrowing.repository;

import com.example.lms.borrowing.model.Issue;
import com.example.lms.borrowing.model.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByUser_UserId(Long userId);

    List<Issue> findByUser_UserIdAndStatus(Long userId, IssueStatus status);
}
