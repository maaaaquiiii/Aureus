package com.aureus.analytics.api;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummary(
        Long userId,
        String period,
        BigDecimal totalSpent,
        BigDecimal totalBudget,
        BigDecimal averagePerDay,
        int transactionCount,
        List<CategorySummary> categories
) {}