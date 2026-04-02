package com.aureus.llm.api;

import java.time.LocalDateTime;

public record LlmResponse(
        Long id,
        String period,
        String analysis,
        String modelUsed,
        Integer tokensUsed,
        LocalDateTime createdAt,
        boolean cached
) {}