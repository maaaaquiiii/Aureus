package com.aureus.ingestion.api;

public record ImportResponse(
        Long jobId,
        String status,
        int totalRows,
        int importedRows,
        String errorDetail
) {}