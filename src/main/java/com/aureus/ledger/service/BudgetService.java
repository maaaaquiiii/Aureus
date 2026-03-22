package com.aureus.ledger.service;

import com.aureus.ledger.api.BudgetRequest;
import com.aureus.ledger.api.BudgetResponse;
import com.aureus.ledger.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public BudgetService(BudgetRepository budgetRepository, UserRepository userRepository, CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.userId()));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.categoryId()));

        // Check if a budget already exists for this user, category, and period (month)
        budgetRepository.findByUserIdAndCategoryIdAndPeriod(request.userId(), request.categoryId(),
                request.period()).ifPresent(b -> {
            throw new RuntimeException("Budget already exists for this category and period");
        });
        // Initialize and populate the new Budget entity
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setPeriod(request.period());
        budget.setLimitAmount(request.limitAmount());
        return toResponse(budgetRepository.save(budget));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> findByUserAndPeriod(Long userId, String period) {
        return budgetRepository.findByUserIdAndPeriod(userId, period)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse updateBudget(Long budgetId, BudgetRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found: " + budgetId));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.categoryId()));

        budget.setCategory(category);
        budget.setPeriod(request.period());
        budget.setLimitAmount(request.limitAmount());

        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(Long budgetId) {
        if (!budgetRepository.existsById(budgetId)) {
            throw new RuntimeException("Budget not found: " + budgetId);
        }
        budgetRepository.deleteById(budgetId);
    }

    private BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getUser().getId(),
                budget.getCategory().getName(),
                budget.getCategory().getColor(),
                budget.getPeriod(),
                budget.getLimitAmount()
        );
    }
}