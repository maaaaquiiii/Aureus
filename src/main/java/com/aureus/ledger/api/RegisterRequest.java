package com.aureus.ledger.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank @Size(min = 6) String password,
        @Size(min = 3, max = 3) String currency
) {}