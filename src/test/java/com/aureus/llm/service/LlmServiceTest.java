package com.aureus.llm.service;

import com.aureus.analytics.api.MonthlySummary;
import com.aureus.analytics.service.AnalyticsService;
import com.aureus.ledger.domain.User;
import com.aureus.ledger.domain.UserRepository;
import com.aureus.llm.api.LlmResponse;
import com.aureus.llm.domain.LlmAnalysis;
import com.aureus.llm.domain.LlmAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmServiceTest {
    @Mock
    private LlmAnalysisRepository llmAnalysisRepository;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private UserRepository userRepository;
    private LlmService llmService;

    void setUp(String apiKey) {
        llmService = new LlmService(llmAnalysisRepository, analyticsService, userRepository, apiKey, "gpt-4.1-mini");
    }

    @Test
    void analyse_whenCacheExists_returnsCachedResultWithoutCallingApi() {
        setUp("placeholder");
        LlmAnalysis cached = buildAnalysis(1L, 1L, "2025-03", "Análisis guardado");
        when(llmAnalysisRepository.findByUserIdAndPeriod(1L, "2025-03"))
                .thenReturn(Optional.of(cached));
        LlmResponse response = llmService.analyse(1L, "2025-03");
        assertThat(response.cached()).isTrue();
        assertThat(response.analysis()).isEqualTo("Análisis guardado");
        assertThat(response.period()).isEqualTo("2025-03");
        verify(analyticsService, never()).calculateMonthlySummary(any(), any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void analyse_whenNoCacheAndApiKeyIsPlaceholder_throwsRuntimeException() {
        setUp("placeholder");
        when(llmAnalysisRepository.findByUserIdAndPeriod(1L, "2025-03"))
                .thenReturn(Optional.empty());
        MonthlySummary summary = new MonthlySummary(1L, "2025-03", new BigDecimal("500.00"), new BigDecimal("600.00"), new BigDecimal("16.13"), 10, List.of());
        when(analyticsService.calculateMonthlySummary(1L, YearMonth.of(2025, 3)))
                .thenReturn(summary);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> llmService.analyse(1L, "2025-03"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error calling OpenAI API");
    }

    @Test
    void getCached_whenExists_returnsResponse() {
        setUp("placeholder");
        LlmAnalysis analysis = buildAnalysis(1L, 1L, "2025-03", "Texto análisis");
        when(llmAnalysisRepository.findByUserIdAndPeriod(1L, "2025-03"))
                .thenReturn(Optional.of(analysis));
        LlmResponse response = llmService.getCached(1L, "2025-03");
        assertThat(response).isNotNull();
        assertThat(response.cached()).isTrue();
        assertThat(response.analysis()).isEqualTo("Texto análisis");
    }

    @Test
    void getCached_whenNotExists_returnsNull() {
        setUp("placeholder");
        when(llmAnalysisRepository.findByUserIdAndPeriod(1L, "2025-03"))
                .thenReturn(Optional.empty());
        LlmResponse response = llmService.getCached(1L, "2025-03");
        assertThat(response).isNull();
    }

    @Test
    void deleteAnalysis_whenExists_deletesIt() {
        setUp("placeholder");
        LlmAnalysis analysis = buildAnalysis(1L, 1L, "2025-03", "Texto");
        when(llmAnalysisRepository.findByUserIdAndPeriod(1L, "2025-03"))
                .thenReturn(Optional.of(analysis));
        llmService.deleteAnalysis(1L, "2025-03");
        verify(llmAnalysisRepository).delete(analysis);
    }

    @Test
    void deleteAnalysis_whenNotExists_doesNothing() {
        setUp("placeholder");
        when(llmAnalysisRepository.findByUserIdAndPeriod(1L, "2025-03"))
                .thenReturn(Optional.empty());
        llmService.deleteAnalysis(1L, "2025-03");
        verify(llmAnalysisRepository, never()).delete(any());
    }

    private LlmAnalysis buildAnalysis(Long id, Long userId, String period, String text) {
        User user = new User();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, userId);
        } catch (Exception ignored) {}
        LlmAnalysis analysis = new LlmAnalysis();
        analysis.setUser(user);
        analysis.setPeriod(period);
        analysis.setAnalysis(text);
        analysis.setPromptUsed("prompt");
        analysis.setModelUsed("gpt-4.1-mini");
        analysis.setTokensUsed(100);
        try {
            var idField = LlmAnalysis.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(analysis, id);
            var createdAtField = LlmAnalysis.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(analysis, LocalDateTime.now());
        } catch (Exception ignored) {}
        return analysis;
    }
}