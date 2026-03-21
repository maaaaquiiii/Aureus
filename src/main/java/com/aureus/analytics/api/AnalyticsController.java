package com.aureus.analytics.api;

import com.aureus.analytics.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/users/{userId}/monthly")
    public MonthlySummary getMonthlySummary(@PathVariable Long userId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return analyticsService.calculateMonthlySummary(userId, month);
    }

    @GetMapping("/users/{userId}/evolution")
    public List<MonthlyEvolution> getEvolution(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "12") int months
    ) {
        return analyticsService.calculateEvolution(userId, months);
    }
}