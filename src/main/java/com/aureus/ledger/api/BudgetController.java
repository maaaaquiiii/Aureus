package com.aureus.ledger.api;

import com.aureus.ledger.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(request));
    }

    @GetMapping("/users/{userId}")
    public List<BudgetResponse> findByUserAndPeriod(@PathVariable Long userId, @RequestParam String period) {
        return budgetService.findByUserAndPeriod(userId, period);
    }

    @PutMapping("/{budgetId}")
    public BudgetResponse updateBudget(@PathVariable Long budgetId, @Valid @RequestBody BudgetRequest request) {
        return budgetService.updateBudget(budgetId, request);
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long budgetId) {
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.noContent().build();
    }
}