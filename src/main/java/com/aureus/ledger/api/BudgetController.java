package com.aureus.ledger.api;

import com.aureus.ledger.service.BudgetService;
import com.aureus.platform.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private final BudgetService budgetService;
    private final JwtService jwtService;

    public BudgetController(BudgetService budgetService, JwtService jwtService) {
        this.budgetService = budgetService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody BudgetRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.createBudget(request, userId));
    }

    @GetMapping("/users/{userId}")
    public List<BudgetResponse> findByUserAndPeriod(
            @PathVariable Long userId,
            @RequestParam String period) {
        return budgetService.findByUserAndPeriod(userId, period);
    }

    @PutMapping("/{budgetId}")
    public BudgetResponse updateBudget(
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetRequest request) {
        return budgetService.updateBudget(budgetId, request);
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long budgetId) {
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.noContent().build();
    }
}