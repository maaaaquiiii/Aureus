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
        // Split by comma, but ignore commas that are inside double quotes
        String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        // Ensure there are enough fields to avoid IndexOutOfBoundsException
        if (fields.length < 8) return null;

        String type        = clean(fields[0]);
        String startedDate = clean(fields[2]);
        String description = clean(fields[4]);
        String amountStr   = clean(fields[5]);
        String currency    = clean(fields[7]);
        String state       = clean(fields[8]);
        // Only process completed transactions
        if (!"COMPLETED".equalsIgnoreCase(state)) return null;
        // Only consider expenses (which usually come as negative amounts in Revolut)
        BigDecimal amount = new BigDecimal(amountStr);
        if (amount.compareTo(BigDecimal.ZERO) >= 0) return null;
        // Convert the negative amount to a positive value for our records
        amount = amount.abs();
        // Parse the transaction date
        LocalDate date = LocalDate.parse(startedDate, FORMATTER);
        // Find the corresponding category by Revolut type, or fallback to the "Other" category
        Category category = categoryRepository
                .findByNameIgnoreCase(mapRevolutType(type))
                .orElseGet(() -> categoryRepository
                        .findByNameIgnoreCase("Other")
                        .orElseThrow(() -> new RuntimeException("Category 'Other' not found")));

        // Build and populate the Expense object
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
            case "CARD_PAYMENT"    -> "Eating Out";
            case "TRANSFER"        -> "Other";
            case "TOPUP"           -> "Other";
            case "ATM"             -> "Other";
            case "SUBSCRIPTION"    -> "Subscriptions";
            case "TRAVEL"          -> "Travel";
            default                -> "Other";
        };
    }

    private String clean(String value) {
        return value.trim().replace("\"", "");
    }
}