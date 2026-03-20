package com.aureus.ledger.api;

public record UserResponse(
        Long id,
        String email,
        String name,
        String currency
) {}