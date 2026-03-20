package com.aureus.ledger.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdAndPeriod(Long userId, String period);
    Optional<Budget> findByUserIdAndCategoryIdAndPeriod(Long userId, Long categoryId, String period);
}