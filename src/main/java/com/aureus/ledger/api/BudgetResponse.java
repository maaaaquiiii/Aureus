package com.aureus.ledger.api;

import java.math.BigDecimal;

public record BudgetResponse(
        Long id,
        Long userId,
        String category,
        String categoryColor,
        String period,
        BigDecimal limitAmount
) {}