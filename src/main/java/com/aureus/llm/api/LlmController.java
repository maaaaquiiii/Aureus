package com.aureus.llm.api;

import com.aureus.llm.service.LlmService;
import com.aureus.platform.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/llm")
public class LlmController {
    private final LlmService llmService;
    private final JwtService jwtService;

    public LlmController(LlmService llmService, JwtService jwtService) {
        this.llmService = llmService;
        this.jwtService = jwtService;
    }

    // Analyse the given period — returns cached result if available
    @PostMapping("/analyse")
    public ResponseEntity<LlmResponse> analyse(
            @Valid @RequestBody LlmRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        return ResponseEntity.ok(llmService.analyse(userId, request.period()));
    }

    // Check if a cached analysis exists without triggering a new API call
    @GetMapping("/analyse")
    public ResponseEntity<LlmResponse> getCached(
            @RequestParam String period,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        LlmResponse cached = llmService.getCached(userId, period);
        return cached != null ? ResponseEntity.ok(cached) : ResponseEntity.noContent().build();
    }

    // Delete a cached analysis to force regeneration on next request
    @DeleteMapping("/analyse")
    public ResponseEntity<Void> deleteAnalysis(
            @RequestParam String period,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        llmService.deleteAnalysis(userId, period);
        return ResponseEntity.noContent().build();
    }
}