package com.aureus.ledger.api;

public record CategoryResponse(
        Long id,
        String name,
        String icon,
        String color
) {}