package com.aureus.analytics.api;

import com.aureus.analytics.service.AnalyticsService;
import com.aureus.platform.security.JwtService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final JwtService jwtService;

    public AnalyticsController(AnalyticsService analyticsService, JwtService jwtService) {
        this.analyticsService = analyticsService;
        this.jwtService = jwtService;
    }

    @GetMapping("/monthly")
    public MonthlySummary getMonthlySummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        return analyticsService.calculateMonthlySummary(userId, month);
    }

    @GetMapping("/evolution")
    public List<MonthlyEvolution> getEvolution(
            @RequestParam(defaultValue = "12") int months,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        return analyticsService.calculateEvolution(userId, months);
    }
}