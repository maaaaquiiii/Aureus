package com.aureus.ledger.api;

import com.aureus.ledger.service.ExpenseService;
import com.aureus.platform.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;
    private final JwtService jwtService;

    public ExpenseController(ExpenseService expenseService, JwtService jwtService) {
        this.expenseService = expenseService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createExpense(request, userId));
    }

    @GetMapping("/users/{userId}/monthly")
    public List<ExpenseResponse> getMonthlyExpenses(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return expenseService.findMonthlyExpenses(userId, month);
    }
}