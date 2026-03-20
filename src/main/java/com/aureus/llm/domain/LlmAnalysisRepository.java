package com.aureus.llm.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LlmAnalysisRepository extends JpaRepository<LlmAnalysis, Long> {
    Optional<LlmAnalysis> findByUserIdAndPeriod(Long userId, String period);
}