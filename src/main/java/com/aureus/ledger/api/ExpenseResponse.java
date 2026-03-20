package com.aureus.ledger.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        Long userId,
        String category,
        String categoryColor,
        BigDecimal amount,
        String currency,
        LocalDate incurredOn,
        String description,
        String source
) {}