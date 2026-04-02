package com.aureus.ingestion.api;

import com.aureus.ingestion.service.ImportService;
import com.aureus.platform.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final ImportService importService;
    private final JwtService jwtService;

    public ImportController(ImportService importService, JwtService jwtService) {
        this.importService = importService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<ImportResponse> importCsv(
            @Valid @RequestBody ImportRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(importService.importCsv(request, userId));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ImportResponse>> getImportJobs(@PathVariable Long userId) {
        return ResponseEntity.ok(importService.getImportJobs(userId));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteImportJob(@PathVariable Long jobId) {
        importService.deleteImportJob(jobId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteAllImportJobs(@PathVariable Long userId) {
        importService.deleteAllImportJobs(userId);
        return ResponseEntity.noContent().build();
    }
}