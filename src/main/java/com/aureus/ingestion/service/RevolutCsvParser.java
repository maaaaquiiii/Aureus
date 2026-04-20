package com.aureus.ingestion.service;

import com.aureus.ledger.domain.Category;
import com.aureus.ledger.domain.CategoryRepository;
import com.aureus.ledger.domain.Expense;
import com.aureus.ledger.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class RevolutCsvParser {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Sentinel value used to mark expenses that need AI categorization
    private static final String NEEDS_AI = "__NEEDS_AI__";
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public RevolutCsvParser(CategoryRepository categoryRepository, @Value("${openai.api-key}") String apiKey, @Value("${openai.model}") String model) {
        this.categoryRepository = categoryRepository;
        this.apiKey = apiKey;
        this.model = model;
    }

    public List<Expense> parse(String csvContent, User user) {
        List<Expense> expenses = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String header = reader.readLine(); // Skip the header row
            if (header == null) return expenses;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    Expense expense = parseLine(line, user);
                    if (expense != null) expenses.add(expense);
                } catch (Exception e) {
                    // Skip lines with invalid format or parsing errors
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing CSV: " + e.getMessage());
        }
        // Collect expenses that could not be categorized by the rule-based mapper
        // and send them to the AI in a single batch call to minimize API costs
        List<Expense> needsAi = expenses.stream()
                .filter(e -> NEEDS_AI.equals(e.getSource()))
                .toList();

        if (!needsAi.isEmpty()) {
            assignCategoriesWithAi(needsAi);
        }
        return expenses;
    }

    private Expense parseLine(String line, User user) {
        // Split on commas that are not inside quoted fields
        String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        if (fields.length < 9) return null;
        String type        = clean(fields[0]);
        String startedDate = clean(fields[2]);
        String description = clean(fields[4]);
        String amountStr   = clean(fields[5]);
        String currency    = clean(fields[7]);
        String state       = clean(fields[8]);
        // Only process completed transactions (supports both English and Spanish CSV exports)
        if (!"COMPLETED".equalsIgnoreCase(state) && !"COMPLETADO".equalsIgnoreCase(state)) return null;
        // Skip internal transfers, top-ups and refunds — they are not real expenses
        if (type.equalsIgnoreCase("TRANSFERIR") || type.equalsIgnoreCase("TRANSFER")) return null;
        if (type.equalsIgnoreCase("RECARGAS") || type.equalsIgnoreCase("TOPUP"))    return null;
        if (type.equalsIgnoreCase("REEMBOLSO DE TARJETA") || type.equalsIgnoreCase("REFUND")) return null;
        // Only import negative amounts (money going out)
        BigDecimal amount = new BigDecimal(amountStr);
        if (amount.compareTo(BigDecimal.ZERO) >= 0) return null;
        amount = amount.abs();
        LocalDate date = LocalDate.parse(startedDate, FORMATTER);
        // For card payments, infer the category from the merchant description;
        // for other types, map directly from the Revolut transaction type
        String categoryName = type.equalsIgnoreCase("PAGO CON TARJETA") ||
                type.equalsIgnoreCase("CARD_PAYMENT")
                ? mapByDescription(description)
                : mapRevolutType(type);
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setAmount(amount);
        expense.setCurrency(currency);
        expense.setIncurredOn(date);
        expense.setDescription(description);
        expense.setExternalId(buildExternalId(startedDate, description, amountStr, currency));
        if (NEEDS_AI.equals(categoryName)) {
            // Mark as pending AI categorization — source is used as a temporary flag
            // and will be replaced with "revolut" after categorization
            expense.setSource(NEEDS_AI);
            // Assign "Other" as a safe fallback in case the AI call fails
            Category other = categoryRepository.findByNameIgnoreCase("Other").orElseThrow();
            expense.setCategory(other);
        } else {
            expense.setSource("revolut");
            Category category = categoryRepository
                    .findByNameIgnoreCase(categoryName)
                    .orElseGet(() -> categoryRepository.findByNameIgnoreCase("Other").orElseThrow());
            expense.setCategory(category);
        }
        return expense;
    }

    // Sends all uncategorized expenses to OpenAI in a single batch request
    private void assignCategoriesWithAi(List<Expense> expenses) {
        try {
            // Build a numbered list of descriptions to send
            StringBuilder prompt = new StringBuilder();
            prompt.append("Clasifica cada descripción de transacción bancaria en una de estas categorías: ")
                    .append("Groceries, Eating Out, Transport, Shopping, Health, Entertainment, Travel, Subscriptions, Education, Other.\n\n");
            prompt.append("Responde ÚNICAMENTE con un objeto JSON con el formato: {\"1\": \"categoria\", \"2\": \"categoria\", ...}\n");
            prompt.append("No incluyas explicaciones ni texto adicional, solo el JSON.\n\n");
            prompt.append("Transacciones:\n");
            for (int i = 0; i < expenses.size(); i++) {
                prompt.append(i + 1).append(". ").append(expenses.get(i).getDescription()).append("\n");
            }
            String requestBody = objectMapper.writeValueAsString(
                    new HashMap<>() {{
                        put("model", model);
                        put("max_tokens", 500);
                        put("messages", List.of(
                                new HashMap<>() {{
                                    put("role", "user");
                                    put("content", prompt.toString());
                                }}
                        ));
                    }}
            );
            // Call the OpenAI API with a single batch request for all uncategorized transactions
            try (HttpClient httpClient = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    // Fail silently — expenses keep "Other" and source is restored
                    expenses.forEach(exp -> exp.setSource("revolut"));
                    return;
                }
                String content = objectMapper.readTree(response.body())
                        .path("choices").get(0).path("message").path("content").asText();
                // Strip markdown code fences if the model wraps the JSON in ```json ... ```
                content = content.replaceAll("```json|```", "").trim();
                @SuppressWarnings("unchecked")
                Map<String, String> result = objectMapper.readValue(content, Map.class);
                // Apply the AI-assigned categories to each expense
                for (int i = 0; i < expenses.size(); i++) {
                    String categoryName = result.get(String.valueOf(i + 1));
                    if (categoryName != null) {
                        categoryRepository.findByNameIgnoreCase(categoryName.trim())
                                .ifPresent(expenses.get(i)::setCategory);
                    }
                    // Restore the real source value now that categorization is done
                    expenses.get(i).setSource("revolut");
                }
            }
        } catch (Exception e) {
            // If the AI call fails, all expenses keep their "Other" fallback category
            // and source is restored so the import can still complete successfully
            expenses.forEach(exp -> exp.setSource("revolut"));
        }
    }

    private String mapRevolutType(String revolutType) {
        return switch (revolutType.toUpperCase()) {
            // English
            case "CARD_PAYMENT" -> "Eating Out";
            case "TRANSFER" -> "Other";
            case "TOPUP" -> "Other";
            case "ATM" -> "Other";
            case "SUBSCRIPTION" -> "Subscriptions";
            case "TRAVEL" -> "Travel";
            // Spanish
            case "PAGO CON TARJETA" -> "Eating Out";
            case "TRANSFERIR" -> "Other";
            case "RECARGAS" -> "Other";
            case "REEMBOLSO DE TARJETA" -> "Other";
            case "CAJERO AUTOMÁTICO" -> "Other";
            default -> "Other";
        };
    }

    private String mapByDescription(String description) {
        String desc = description.toLowerCase();
        if (containsAny(desc, "mercadona", "lidl", "carrefour", "aldi", "dia ", "mp dia", "eroski", "alcampo", "hipercor",
                "consum", "supermercado")) return "Groceries";
        if (containsAny(desc, "starbucks", "just eat", "mcdonald", "burger", "telepizza", "glovo", "uber eats",
                "deliveroo", "domino", "kfc", "subway", "gilderia", "tilabakes", "asgaya", "wine market",
                "pasteleria", "bombon boss", "barbacoa garden", "caffè nero", "cafeteria",
                "casa garcia", "gastro sushi", "krispy kreme", "la belga", "red dragon", "san&dra cheesecake",
                "manolo bakes", "avoca", "mccambridge")) return "Eating Out";
        if (containsAny(desc, "spotify", "netflix", "hbo", "disney", "amazon prime", "youtube", "apple", "apple one",
                "icloud", "microsoft", "google one", "adobe", "chatgpt", "openai", "pearson")) return "Subscriptions";
        if (containsAny(desc, "uber", "cabify", "bolt", "renfe", "metro", "emt", "blablacar", "consorcio regional de transportes", "national transport authority",
                "parking", "gasolinera", "repsol", "bp ", "cepsa")) return "Transport";
        if (containsAny(desc, "zara", "h&m", "mango", "primark", "primaprix", "bershka", "pull&bear", "stradivarius",
                "massimo dutti", "el corte ingles", "el corte inglés", "amazon", "aliexpress", "shein", "asos", "normal",
                "fnac", "media markt", "on stage", "notino", "druni", "tk maxx", "bernabéu market", "bernabeu market", "sp mysticalluxe", "awx dolaka", "muy mucho", "expenduria", "expendedurias",
                "tabacos", "tabaco", "estanco")) return "Shopping";
        if (containsAny(desc, "farmacia", "clinica", "dentista", "medico", "hospital", "optica", "óptica",
                "visionlab", "soloptical", "sanitas", "adeslas", "gym", "gimnasio", "doctor", "fisio")) return "Health";
        if (containsAny(desc, "cine", "teatro", "concierto", "ticketmaster", "entradas", "steam", "playstation",
                "xbox", "nintendo", "escape room")) return "Entertainment";
        if (containsAny(desc, "hotel", "booking", "airbnb", "hostel", "trivago", "expedia", "viaje", "iberia", "aena", "ryanair", "vueling")) return "Travel";
        if (containsAny(desc, "udemy", "coursera", "libro", "libreria", "academia", "escuela", "universidad", "english connection")) return "Education";
        // Description not recognized — mark for AI categorization
        return NEEDS_AI;
    }

    private String buildExternalId(String startedDate, String description, String amount, String currency) {
        String raw = startedDate + "|" + description + "|" + amount + "|" + currency;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash); // 64-character hex string
        } catch (NoSuchAlgorithmException e) {
            return raw;
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String clean(String value) {
        return value.trim().replace("\"", "");
    }
}