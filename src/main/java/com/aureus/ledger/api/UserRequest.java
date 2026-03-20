package com.aureus.ledger.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @Size(min = 3, max = 3) String currency
) {}