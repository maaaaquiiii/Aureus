package com.aureus.ledger.api;

import com.aureus.ledger.service.ExpenseService;
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

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createExpense(request));
    }

    @GetMapping("/users/{userId}/monthly")
    public List<ExpenseResponse> getMonthlyExpenses(@PathVariable Long userId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return expenseService.findMonthlyExpenses(userId, month);
    }
}