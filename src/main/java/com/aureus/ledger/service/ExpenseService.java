package com.aureus.ledger.service;

import com.aureus.ledger.api.ExpenseRequest;
import com.aureus.ledger.api.ExpenseResponse;
import com.aureus.ledger.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.YearMonth;
import java.util.List;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository, CategoryRepository categoryRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.userId()));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.categoryId()));

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setAmount(request.amount());
        expense.setCurrency(request.currency());
        expense.setIncurredOn(request.incurredOn());
        expense.setDescription(request.description());
        expense.setSource(request.source());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findMonthlyExpenses(Long userId, YearMonth month) {
        return expenseRepository.findByUserIdAndIncurredOnBetweenOrderByIncurredOnDesc(
                        userId,
                        month.atDay(1),
                        month.atEndOfMonth()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getUser().getId(),
                expense.getCategory().getName(),
                expense.getCategory().getColor(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getIncurredOn(),
                expense.getDescription(),
                expense.getSource()
        );
    }
}