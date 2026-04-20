package com.aureus.ledger.service;

import com.aureus.ledger.api.ExpenseRequest;
import com.aureus.ledger.api.ExpenseResponse;
import com.aureus.ledger.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {
    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void createExpense_withValidData_returnsExpenseResponse() {
        User user = buildUser(1L);
        Category category = buildCategory(2L, "Groceries", "#34d399");
        Expense saved = buildExpense(10L, user, category, new BigDecimal("45.00"),
                LocalDate.of(2025, 3, 15));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenReturn(saved);
        ExpenseRequest request = new ExpenseRequest(2L, new BigDecimal("45.00"), "EUR", LocalDate.of(2025, 3, 15), "Mercadona", "manual");
        ExpenseResponse response = expenseService.createExpense(request, 1L);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.category()).isEqualTo("Groceries");
        assertThat(response.amount()).isEqualByComparingTo("45.00");
    }

    @Test
    void createExpense_whenUserNotFound_throwsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        ExpenseRequest request = new ExpenseRequest(2L, new BigDecimal("10.00"), "EUR", LocalDate.now(), "Test", "manual");
        assertThatThrownBy(() -> expenseService.createExpense(request, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createExpense_whenCategoryNotFound_throwsRuntimeException() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        ExpenseRequest request = new ExpenseRequest(99L, new BigDecimal("10.00"), "EUR", LocalDate.now(), "Test", "manual");
        assertThatThrownBy(() -> expenseService.createExpense(request, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void findMonthlyExpenses_returnsMappedList() {
        User user = buildUser(1L);
        Category cat = buildCategory(2L, "Eating Out", "#f87171");
        Expense expense = buildExpense(1L, user, cat, new BigDecimal("20.00"), LocalDate.of(2025, 3, 10));
        when(expenseRepository.findByUserIdAndIncurredOnBetweenOrderByIncurredOnDesc(1L, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)))
                .thenReturn(List.of(expense));
        List<ExpenseResponse> result = expenseService.findMonthlyExpenses(1L, YearMonth.of(2025, 3));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("Eating Out");
        assertThat(result.get(0).amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void findMonthlyExpenses_whenNoExpenses_returnsEmptyList() {
        when(expenseRepository.findByUserIdAndIncurredOnBetweenOrderByIncurredOnDesc(any(), any(), any()))
                .thenReturn(List.of());
        List<ExpenseResponse> result = expenseService.findMonthlyExpenses(1L, YearMonth.of(2025, 3));
        assertThat(result).isEmpty();
    }

    @Test
    void updateCategory_withValidIds_updatesCategory() {
        User user = buildUser(1L);
        Category oldCat = buildCategory(2L, "Other", "#64748b");
        Category newCat = buildCategory(3L, "Groceries", "#34d399");
        Expense expense = buildExpense(10L, user, oldCat, new BigDecimal("30.00"), LocalDate.now());
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(expense));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(newCat));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);
        expenseService.updateCategory(10L, 3L);
        verify(expenseRepository).save(argThat(e -> "Groceries".equals(e.getCategory().getName())));
    }

    @Test
    void updateCategory_whenExpenseNotFound_throwsRuntimeException() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> expenseService.updateCategory(99L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Expense not found");
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

    private Expense buildExpense(Long id, User user, Category category, BigDecimal amount, LocalDate date) {
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setAmount(amount);
        expense.setCurrency("EUR");
        expense.setIncurredOn(date);
        expense.setDescription("Test expense");
        expense.setSource("manual");
        try {
            var field = Expense.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(expense, id);
        } catch (Exception ignored) {}
        return expense;
    }
}