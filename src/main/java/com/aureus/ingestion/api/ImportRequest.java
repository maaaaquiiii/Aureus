package com.aureus.ingestion.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImportRequest(
        @NotNull Long userId,
        @NotBlank String source,
        @NotBlank String csvContent,
        String fileName
) {}