package com.aureus.ingestion.api;

import java.time.LocalDateTime;

public record ImportResponse(
        Long jobId,
        String status,
        int totalRows,
        int importedRows,
        String errorDetail,
        String fileName,
        LocalDateTime createdAt
) {}