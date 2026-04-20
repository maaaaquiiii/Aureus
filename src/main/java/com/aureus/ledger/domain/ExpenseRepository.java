package com.aureus.ledger.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserIdAndIncurredOnBetweenOrderByIncurredOnDesc(Long userId, LocalDate start, LocalDate end);
    List<Expense> findByImportJobId(Long importJobId);
    long countByUserId(Long userId);

    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.incurredOn >= :start AND e.incurredOn <= :end")
    List<Expense> findByUserIdAndIncurredOnBetween(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    void deleteByImportJobId(Long importJobId);
    boolean existsByImportJobId(Long importJobId);
    // Used by ImportService to skip transactions that have already been imported
    boolean existsByUserIdAndExternalId(Long userId, String externalId);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId")
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);
}