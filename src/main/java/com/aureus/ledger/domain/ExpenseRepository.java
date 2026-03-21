package com.aureus.ledger.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserIdAndIncurredOnBetweenOrderByIncurredOnDesc(
            Long userId, LocalDate start, LocalDate end);
    List<Expense> findByImportJobId(Long importJobId);

    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId " +
            "AND e.incurredOn >= :start AND e.incurredOn <= :end")
    List<Expense> findByUserIdAndIncurredOnBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}