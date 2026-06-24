package com.example.lms.fines.service;

import com.example.lms.borrowing.model.Issue;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FineService {

    private static final BigDecimal FINE_PER_DAY = new BigDecimal("5.00");

    public BigDecimal calculateFine(Issue issue) {
        return issue.calculateFine(FINE_PER_DAY);
    }
}
