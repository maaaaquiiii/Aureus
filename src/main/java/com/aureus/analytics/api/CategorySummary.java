package com.aureus.analytics.api;

import java.math.BigDecimal;

public record CategorySummary(
        String category,
        String color,
        BigDecimal spent,
        BigDecimal budget,
        BigDecimal remaining
) {}