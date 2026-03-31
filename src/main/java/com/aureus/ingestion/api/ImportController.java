package com.aureus.ingestion.api;

import com.aureus.ingestion.service.ImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping
    public ResponseEntity<ImportResponse> importCsv(@Valid @RequestBody ImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(importService.importCsv(request));
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