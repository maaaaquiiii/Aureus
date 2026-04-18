package com.aureus.llm.service;

import com.aureus.analytics.api.CategorySummary;
import com.aureus.analytics.service.AnalyticsService;
import com.aureus.ledger.domain.UserRepository;
import com.aureus.llm.api.LlmResponse;
import com.aureus.llm.domain.LlmAnalysis;
import com.aureus.llm.domain.LlmAnalysisRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.YearMonth;
import java.util.List;

@Service
public class LlmService {
    private final LlmAnalysisRepository llmAnalysisRepository;
    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public LlmService(LlmAnalysisRepository llmAnalysisRepository, AnalyticsService analyticsService, UserRepository userRepository, @Value("${openai.api-key}") String apiKey, @Value("${openai.model}") String model) {
        this.llmAnalysisRepository = llmAnalysisRepository;
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Transactional
    public LlmResponse analyse(Long userId, String period) {
        // Return cached analysis if one already exists for this user and period
        var existing = llmAnalysisRepository.findByUserIdAndPeriod(userId, period);
        if (existing.isPresent()) {
            LlmAnalysis cached = existing.get();
            return toResponse(cached, true);
        }
        // Build the prompt from the user's real monthly data
        YearMonth yearMonth = YearMonth.parse(period);
        var summary = analyticsService.calculateMonthlySummary(userId, yearMonth);
        String prompt = buildPrompt(period, summary.totalSpent().doubleValue(),
                summary.totalBudget().doubleValue(), summary.transactionCount(),
                summary.categories());
        // Call the OpenAI API
        String analysisText;
        int tokensUsed;
        try {
            var result = callOpenAi(prompt);
            analysisText = result[0];
            tokensUsed = Integer.parseInt(result[1]);
        } catch (Exception e) {
            throw new RuntimeException("Error calling OpenAI API: " + e.getMessage());
        }
        // Persist the result so we don't call the API again for the same period
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        LlmAnalysis analysis = new LlmAnalysis();
        analysis.setUser(user);
        analysis.setPeriod(period);
        analysis.setPromptUsed(prompt);
        analysis.setAnalysis(analysisText);
        analysis.setModelUsed(model);
        analysis.setTokensUsed(tokensUsed);
        llmAnalysisRepository.save(analysis);
        return toResponse(analysis, false);
    }

    private String buildPrompt(String period, double totalSpent, double totalBudget, int transactionCount, List<CategorySummary> categories) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un asistente de finanzas personales. Analiza los gastos del usuario para el período ")
                .append(period).append(" y proporciona insights útiles y personalizados en español.\n\n");
        sb.append("DATOS DEL MES:\n");
        sb.append("- Total gastado: ").append(String.format("%.2f", totalSpent)).append(" €\n");
        if (totalBudget > 0) {
            sb.append("- Presupuesto total: ").append(String.format("%.2f", totalBudget)).append(" €\n");
            double pct = (totalSpent / totalBudget) * 100;
            sb.append("- Uso del presupuesto: ").append(String.format("%.1f", pct)).append("%\n");
        }
        sb.append("- Número de transacciones: ").append(transactionCount).append("\n\n");
        sb.append("GASTOS POR CATEGORÍA:\n");
        categories.stream()
                .sorted((a, b) -> b.spent().compareTo(a.spent()))
                .forEach(cat -> {
                    sb.append("- ").append(cat.category()).append(": ")
                            .append(String.format("%.2f", cat.spent().doubleValue())).append(" €");
                    if (cat.budget().doubleValue() > 0) {
                        sb.append(" (presupuesto: ").append(String.format("%.2f", cat.budget().doubleValue())).append(" €)");
                    }
                    sb.append("\n");
                });
        sb.append("\nPor favor proporciona:\n");
        sb.append("1. Un resumen breve del mes\n");
        sb.append("2. Las 2-3 categorías donde más se ha gastado y si es preocupante\n");
        sb.append("3. Si hay presupuestos, indica cuáles se han superado\n");
        sb.append("4. 2-3 recomendaciones concretas y accionables para el próximo mes\n");
        sb.append("5. Una valoración general del mes (positiva, neutral o negativa)\n\n");
        sb.append("Responde de forma clara, directa y en un tono amigable pero profesional. Usa formato markdown.\n");
        sb.append("No incluyas frases de cierre del tipo 'si quieres puedo ayudarte' o similares. Termina el análisis directamente después de la valoración general.");
        return sb.toString();
    }

    private String[] callOpenAi(String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("model", model);
                    put("max_tokens", 1000);
                    put("messages", List.of(
                            new java.util.HashMap<>() {{
                                put("role", "user");
                                put("content", prompt);
                            }}
                    ));
                }}
        );
        // Call the OpenAI API and parse the response
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("OpenAI returned status " + response.statusCode() + ": " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            int tokens = root.path("usage").path("total_tokens").asInt();
            return new String[]{ content, String.valueOf(tokens) };
        }
    }

    @Transactional(readOnly = true)
    public LlmResponse getCached(Long userId, String period) {
        return llmAnalysisRepository.findByUserIdAndPeriod(userId, period)
                .map(a -> toResponse(a, true))
                .orElse(null);
    }

    @Transactional
    public void deleteAnalysis(Long userId, String period) {
        llmAnalysisRepository.findByUserIdAndPeriod(userId, period)
                .ifPresent(llmAnalysisRepository::delete);
    }

    private LlmResponse toResponse(LlmAnalysis analysis, boolean cached) {
        return new LlmResponse(
                analysis.getId(),
                analysis.getPeriod(),
                analysis.getAnalysis(),
                analysis.getModelUsed(),
                analysis.getTokensUsed(),
                analysis.getCreatedAt(),
                cached
        );
    }
}