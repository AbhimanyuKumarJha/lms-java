package com.example.lms.borrowing.controllers;

import com.example.lms.borrowing.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    public IssueResponse createIssue(@Valid @RequestBody CreateIssueRequest request) {
        return issueService.createIssue(request);
    }

    @PostMapping("/bulk")
    public List<IssueResponse> createIssues(@Valid @RequestBody List<CreateIssueRequest> requests) {
        return issueService.createIssues(requests);
    }

    @PostMapping("/{transactionId}/return")
    public IssueResponse returnBook(@PathVariable Long transactionId) {
        return issueService.returnBook(transactionId);
    }

    @GetMapping
    public List<IssueResponse> getIssues() {
        return issueService.getAllIssues();
    }
}
