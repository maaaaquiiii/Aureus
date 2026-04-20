package com.aureus.ledger.service;

import com.aureus.ingestion.domain.ImportJobRepository;
import com.aureus.ledger.api.UserResponse;
import com.aureus.ledger.api.UserStatsResponse;
import com.aureus.ledger.api.UserUpdateRequest;
import com.aureus.ledger.domain.ExpenseRepository;
import com.aureus.ledger.domain.User;
import com.aureus.ledger.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ImportJobRepository importJobRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void updateUser_withNewPassword_encodesAndSaves() {
        User user = buildUser(1L, "test@email.com", "Macarena", "EUR");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nuevapass")).thenReturn("nuevo-hash");
        when(userRepository.save(any(User.class))).thenReturn(user);
        userService.updateUser(1L, new UserUpdateRequest("nuevapass", null));
        verify(passwordEncoder).encode("nuevapass");
        verify(userRepository).save(argThat(u -> "nuevo-hash".equals(u.getPasswordHash())));
    }

    @Test
    void updateUser_withNewCurrency_updatesAndSaves() {
        User user = buildUser(1L, "test@email.com", "Macarena", "EUR");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        userService.updateUser(1L, new UserUpdateRequest(null, "USD"));
        verify(userRepository).save(argThat(u -> "USD".equals(u.getCurrency())));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUser_withBlankPassword_doesNotEncodePassword() {
        User user = buildUser(1L, "test@email.com", "Macarena", "EUR");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        userService.updateUser(1L, new UserUpdateRequest("   ", "GBP"));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUser_whenUserNotFound_throwsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUser(99L, new UserUpdateRequest("pass", "EUR")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getStats_returnsCorrectValues() {
        User user = buildUser(1L, "test@email.com", "Macarena", "EUR");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(expenseRepository.countByUserId(1L)).thenReturn(35L);
        when(importJobRepository.countByUserId(1L)).thenReturn(3L);
        when(expenseRepository.sumAmountByUserId(1L)).thenReturn(new BigDecimal("772.65"));
        UserStatsResponse stats = userService.getStats(1L);
        assertThat(stats.totalExpenses()).isEqualTo(35L);
        assertThat(stats.totalImports()).isEqualTo(3L);
        assertThat(stats.totalSpent()).isEqualByComparingTo("772.65");
        assertThat(stats.memberSince()).isNotBlank();
    }

    @Test
    void getStats_whenNoExpenses_returnsTotalSpentZero() {
        User user = buildUser(1L, "test@email.com", "Macarena", "EUR");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(expenseRepository.countByUserId(1L)).thenReturn(0L);
        when(importJobRepository.countByUserId(1L)).thenReturn(0L);
        when(expenseRepository.sumAmountByUserId(1L)).thenReturn(null);
        UserStatsResponse stats = userService.getStats(1L);
        assertThat(stats.totalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findById_whenExists_returnsResponse() {
        User user = buildUser(1L, "test@email.com", "Macarena", "EUR");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserResponse response = userService.findById(1L);
        assertThat(response.email()).isEqualTo("test@email.com");
        assertThat(response.name()).isEqualTo("Macarena");
    }

    @Test
    void findById_whenNotFound_throwsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    private User buildUser(Long id, String email, String name, String currency) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setCurrency(currency);
        user.setPasswordHash("hashed");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
            var createdAtField = User.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(user, LocalDateTime.now());
        } catch (Exception ignored) {}
        return user;
    }
}