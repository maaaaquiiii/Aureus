package com.aureus.ledger.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record BudgetRequest(
        @NotNull Long userId,
        @NotNull Long categoryId,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period,
        @NotNull @DecimalMin("0.01") BigDecimal limitAmount
) {}