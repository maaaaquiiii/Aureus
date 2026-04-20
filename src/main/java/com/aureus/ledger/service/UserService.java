package com.aureus.ledger.service;

import com.aureus.ingestion.domain.ImportJobRepository;
import com.aureus.ledger.api.UserRequest;
import com.aureus.ledger.api.UserResponse;
import com.aureus.ledger.api.UserStatsResponse;
import com.aureus.ledger.api.UserUpdateRequest;
import com.aureus.ledger.domain.ExpenseRepository;
import com.aureus.ledger.domain.User;
import com.aureus.ledger.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final ImportJobRepository importJobRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ExpenseRepository expenseRepository, ImportJobRepository importJobRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.importJobRepository = importJobRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        if (request.currency() != null) {
            user.setCurrency(request.currency());
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            user.setCurrency(request.currency());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getStats(Long userId) {
        long totalExpenses = expenseRepository.countByUserId(userId);
        long totalImports  = importJobRepository.countByUserId(userId);
        BigDecimal totalSpent = expenseRepository.sumAmountByUserId(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        String memberSince = user.getCreatedAt()
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es")));
        return new UserStatsResponse(
                totalExpenses,
                totalImports,
                totalSpent != null ? totalSpent : BigDecimal.ZERO,
                memberSince
        );
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCurrency()
        );
    }
}