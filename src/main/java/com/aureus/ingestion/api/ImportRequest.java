package com.aureus.ingestion.api;

import jakarta.validation.constraints.NotBlank;

public record ImportRequest(
        @NotBlank String source,
        @NotBlank String csvContent,
        String fileName
) {}