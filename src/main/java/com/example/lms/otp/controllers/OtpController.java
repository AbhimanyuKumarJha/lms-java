package com.example.lms.otp.controllers;

import com.example.lms.otp.service.OtpService;
import com.example.lms.users.controllers.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/verify")
    public UserResponse verify(@Valid @RequestBody VerifyOtpRequest request) {
        return otpService.verifySignupOtp(request);
    }
}
