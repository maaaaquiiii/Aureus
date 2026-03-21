package com.aureus.analytics.api;

import java.math.BigDecimal;

public record MonthlyEvolution(
        String period,
        BigDecimal totalSpent,
        int transactionCount
) {}