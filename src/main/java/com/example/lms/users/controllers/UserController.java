package com.example.lms.users.controllers;

import com.example.lms.borrowing.controllers.IssueResponse;
import com.example.lms.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    public List<UserResponse> getUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{userId}/issued-books")
    public List<IssueResponse> getIssuedBooks(@PathVariable Long userId) {
        return userService.getIssuedBooks(userId);
    }

    @GetMapping("/{userId}/fine")
    public BigDecimal getTotalFine(@PathVariable Long userId) {
        return userService.getTotalFine(userId);
    }
}
