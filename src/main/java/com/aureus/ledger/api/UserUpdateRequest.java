package com.aureus.ledger.api;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,
        @Size(min = 3, max = 3, message = "La moneda debe tener 3 caracteres")
        String currency
) {}