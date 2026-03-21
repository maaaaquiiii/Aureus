package com.aureus.analytics.service;

import com.aureus.analytics.api.CategorySummary;
import com.aureus.analytics.api.MonthlyEvolution;
import com.aureus.analytics.api.MonthlySummary;
import com.aureus.ledger.domain.BudgetRepository;
import com.aureus.ledger.domain.Expense;
import com.aureus.ledger.domain.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public AnalyticsService(ExpenseRepository expenseRepository, BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public MonthlySummary calculateMonthlySummary(Long userId, YearMonth month) {
        var expenses = expenseRepository.findByUserIdAndIncurredOnBetweenOrderByIncurredOnDesc(
                userId,
                month.atDay(1),
                month.atEndOfMonth()
        );
        var budgets = budgetRepository.findByUserIdAndPeriod(userId, month.toString());
        // Map of categoryName -> limitAmount
        Map<String, BigDecimal> budgetMap = budgets.stream()
                .collect(Collectors.toMap(
                        b -> b.getCategory().getName(),
                        b -> b.getLimitAmount()
                ));
        // Group expenses by category
        Map<String, List<com.aureus.ledger.domain.Expense>> byCategory = expenses.stream()
                .collect(Collectors.groupingBy(e -> e.getCategory().getName()));
        // Build CategorySummary for each category with expenses
        List<CategorySummary> categories = byCategory.entrySet().stream()
                .map(entry -> {
                    String categoryName = entry.getKey();
                    String color = entry.getValue().get(0).getCategory().getColor();
                    BigDecimal spent = entry.getValue().stream()
                            .map(com.aureus.ledger.domain.Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal budget = budgetMap.getOrDefault(categoryName, BigDecimal.ZERO);
                    BigDecimal remaining = budget.subtract(spent);
                    return new CategorySummary(categoryName, color, spent, budget, remaining);
                })
                .toList();
        BigDecimal totalSpent = expenses.stream()
                .map(com.aureus.ledger.domain.Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBudget = budgets.stream()
                .map(b -> b.getLimitAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averagePerDay = expenses.isEmpty() ? BigDecimal.ZERO :
                totalSpent.divide(BigDecimal.valueOf(month.lengthOfMonth()), 2, RoundingMode.HALF_UP);
        return new MonthlySummary(
                userId,
                month.toString(),
                totalSpent,
                totalBudget,
                averagePerDay,
                expenses.size(),
                categories
        );
    }

    @Transactional(readOnly = true)
    public List<MonthlyEvolution> calculateEvolution(Long userId, int months) {
        YearMonth current = YearMonth.now();
        List<MonthlyEvolution> evolution = new ArrayList<>();

        // Loop backwards to build the list in chronological order (from oldest to newest)
        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            // Retrieve all expenses for the user within that specific month
            var expenses = expenseRepository.findByUserIdAndIncurredOnBetweenOrderByIncurredOnDesc(
                    userId,
                    month.atDay(1),
                    month.atEndOfMonth()
            );
            // Calculate the total amount spent during the month
            BigDecimal totalSpent = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Create and add the monthly evolution record to the list
            evolution.add(new MonthlyEvolution(
                    month.toString(),
                    totalSpent,
                    expenses.size()
            ));
        }
        return evolution;
    }
}