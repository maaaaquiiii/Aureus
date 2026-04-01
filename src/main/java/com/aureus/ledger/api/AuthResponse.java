package com.aureus.ledger.api;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String name
) {}
