package com.aureus.llm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LlmRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period
) {}