package com.aureus.ingestion.service;

import com.aureus.ledger.domain.Category;
import com.aureus.ledger.domain.CategoryRepository;
import com.aureus.ledger.domain.Expense;
import com.aureus.ledger.domain.User;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
@Component
public class RevolutCsvParser {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CategoryRepository categoryRepository;

    public RevolutCsvParser(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
                    if (expense != null) {
                        expenses.add(expense);
                    }
                } catch (Exception e) {
                    // Skip lines with invalid format or parsing errors
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing CSV: " + e.getMessage());
        }
        return expenses;
    }

    private Expense parseLine(String line, User user) {
        String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        if (fields.length < 8) return null;
        String type = clean(fields[0]);
        String startedDate = clean(fields[2]);
        String description = clean(fields[4]);
        String amountStr = clean(fields[5]);
        String currency = clean(fields[7]);
        String state = clean(fields[8]);

        if (!"COMPLETED".equalsIgnoreCase(state) && !"COMPLETADO".equalsIgnoreCase(state)) return null;
        if (type.equalsIgnoreCase("TRANSFERIR") ||
                type.equalsIgnoreCase("TRANSFER")) return null;
        if (type.equalsIgnoreCase("RECARGAS") ||
                type.equalsIgnoreCase("TOPUP")) return null;
        if (type.equalsIgnoreCase("REEMBOLSO DE TARJETA") ||
                type.equalsIgnoreCase("REFUND")) return null;
        BigDecimal amount = new BigDecimal(amountStr);
        if (amount.compareTo(BigDecimal.ZERO) >= 0) return null;
        amount = amount.abs();
        LocalDate date = LocalDate.parse(startedDate, FORMATTER);
        String categoryName = type.equalsIgnoreCase("PAGO CON TARJETA") ||
                type.equalsIgnoreCase("CARD_PAYMENT")
                ? mapByDescription(description)
                : mapRevolutType(type);
        Category category = categoryRepository
                .findByNameIgnoreCase(categoryName)
                .orElseGet(() -> categoryRepository
                        .findByNameIgnoreCase("Other")
                        .orElseThrow());
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setAmount(amount);
        expense.setCurrency(currency);
        expense.setIncurredOn(date);
        expense.setDescription(description);
        expense.setSource("revolut");
        return expense;
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
            // Spanis
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
        if (containsAny(desc, "uber", "cabify", "bolt", "renfe", "metro", "emt", "aena", "ryanair", "vueling", "blablacar", "consorcio regional de transportes", "national transport authority",
                "parking", "gasolinera", "repsol", "bp ", "cepsa")) return "Transport";
        if (containsAny(desc, "zara", "h&m", "mango", "primark", "primaprix", "bershka", "pull&bear", "stradivarius",
                "massimo dutti", "el corte ingles", "el corte inglés", "amazon", "aliexpress", "shein", "asos", "normal ",
                "fnac", "media markt", "on stage", "notino", "druni", "tk maxx", "bernabéu market", "bernabeu market", "sp mysticalluxe", "awx dolaka", "muy mucho", "expenduria", "expendedurias",
                "tabacos", "tabaco", "estanco")) return "Shopping";
        if (containsAny(desc, "farmacia", "clinica", "dentista", "medico", "hospital", "optica", "óptica",
                "visionlab", "soloptical", "sanitas", "adeslas", "gym", "gimnasio", "doctor", "fisio")) return "Health";
        if (containsAny(desc, "cine", "teatro", "concierto", "ticketmaster", "entradas", "steam", "playstation",
                "xbox", "nintendo", "escape room")) return "Entertainment";
        if (containsAny(desc, "hotel", "booking", "airbnb", "hostel", "trivago", "expedia", "viaje", "iberia")) return "Travel";
        if (containsAny(desc, "udemy", "coursera", "libro", "libreria", "academia", "escuela", "universidad", "english connection")) return "Education";
        return "Other";
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