package com.aureus.ledger.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password
) {}