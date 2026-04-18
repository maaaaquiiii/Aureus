package com.aureus.ledger.api;

import java.math.BigDecimal;

public record UserStatsResponse(
        long totalExpenses,
        long totalImports,
        BigDecimal totalSpent,
        String memberSince
) {}