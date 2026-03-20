package com.aureus.ledger.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserIdAndIncurredOnBetweenOrderByIncurredOnDesc(
            Long userId, LocalDate start, LocalDate end);
    List<Expense> findByImportJobId(Long importJobId);
}