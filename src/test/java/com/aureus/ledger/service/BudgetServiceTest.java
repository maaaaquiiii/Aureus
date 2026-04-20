package com.aureus.ledger.service;

import com.aureus.ledger.api.BudgetRequest;
import com.aureus.ledger.api.BudgetResponse;
import com.aureus.ledger.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
    @Mock private BudgetRepository budgetRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks
    private BudgetService budgetService;

    @Test
    void createBudget_withValidData_returnsBudgetResponse() {
        User user = buildUser(1L);
        Category category = buildCategory(2L, "Groceries", "#34d399");
        Budget saved = buildBudget(10L, user, category, "2025-03", new BigDecimal("300.00"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryIdAndPeriod(1L, 2L, "2025-03"))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenReturn(saved);
        BudgetRequest request = new BudgetRequest(2L, "2025-03", new BigDecimal("300.00"));
        BudgetResponse response = budgetService.createBudget(request, 1L);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.category()).isEqualTo("Groceries");
        assertThat(response.limitAmount()).isEqualByComparingTo("300.00");
        assertThat(response.period()).isEqualTo("2025-03");
    }

    @Test
    void createBudget_whenBudgetAlreadyExists_throwsRuntimeException() {
        User user = buildUser(1L);
        Category category = buildCategory(2L, "Groceries", "#34d399");
        Budget existing = buildBudget(5L, user, category, "2025-03", new BigDecimal("200.00"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryIdAndPeriod(1L, 2L, "2025-03"))
                .thenReturn(Optional.of(existing));
        BudgetRequest request = new BudgetRequest(2L, "2025-03", new BigDecimal("300.00"));
        assertThatThrownBy(() -> budgetService.createBudget(request, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Budget already exists");
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void createBudget_whenUserNotFound_throwsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        BudgetRequest request = new BudgetRequest(2L, "2025-03", new BigDecimal("300.00"));
        assertThatThrownBy(() -> budgetService.createBudget(request, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void findByUserAndPeriod_returnsMappedList() {
        User user = buildUser(1L);
        Category cat = buildCategory(2L, "Transport", "#60a5fa");
        Budget budget = buildBudget(1L, user, cat, "2025-03", new BigDecimal("150.00"));
        when(budgetRepository.findByUserIdAndPeriod(1L, "2025-03"))
                .thenReturn(List.of(budget));
        List<BudgetResponse> result = budgetService.findByUserAndPeriod(1L, "2025-03");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("Transport");
    }

    @Test
    void findByUserAndPeriod_whenNoBudgets_returnsEmptyList() {
        when(budgetRepository.findByUserIdAndPeriod(1L, "2025-03"))
                .thenReturn(List.of());
        List<BudgetResponse> result = budgetService.findByUserAndPeriod(1L, "2025-03");
        assertThat(result).isEmpty();
    }

    @Test
    void updateBudget_withValidData_returnsUpdatedResponse() {
        User user = buildUser(1L);
        Category oldCat = buildCategory(2L, "Transport", "#60a5fa");
        Category newCat = buildCategory(3L, "Shopping", "#a78bfa");
        Budget existing = buildBudget(10L, user, oldCat, "2025-03", new BigDecimal("150.00"));
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(newCat));
        when(budgetRepository.save(any(Budget.class))).thenReturn(existing);
        BudgetRequest request = new BudgetRequest(3L, "2025-03", new BigDecimal("200.00"));
        BudgetResponse response = budgetService.updateBudget(10L, request);
        assertThat(response.category()).isEqualTo("Shopping");
    }

    @Test
    void updateBudget_whenBudgetNotFound_throwsRuntimeException() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());
        BudgetRequest request = new BudgetRequest(2L, "2025-03", new BigDecimal("100.00"));
        assertThatThrownBy(() -> budgetService.updateBudget(99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Budget not found");
    }

    @Test
    void deleteBudget_whenExists_deletesIt() {
        when(budgetRepository.existsById(10L)).thenReturn(true);
        budgetService.deleteBudget(10L);
        verify(budgetRepository).deleteById(10L);
    }

    @Test
    void deleteBudget_whenNotFound_throwsRuntimeException() {
        when(budgetRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> budgetService.deleteBudget(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Budget not found");
        verify(budgetRepository, never()).deleteById(any());
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setEmail("test@email.com");
        user.setName("Test");
        user.setCurrency("EUR");
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {}
        return user;
    }

    private Category buildCategory(Long id, String name, String color) {
        Category cat = new Category();
        cat.setName(name);
        cat.setColor(color);
        try {
            var field = Category.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(cat, id);
        } catch (Exception ignored) {}
        return cat;
    }

    private Budget buildBudget(Long id, User user, Category category, String period, BigDecimal limit) {
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setPeriod(period);
        budget.setLimitAmount(limit);
        try {
            var field = Budget.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(budget, id);
        } catch (Exception ignored) {}
        return budget;
    }
}