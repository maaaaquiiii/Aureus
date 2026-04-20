package com.aureus.ingestion.service;

import com.aureus.ingestion.api.ImportRequest;
import com.aureus.ingestion.api.ImportResponse;
import com.aureus.ingestion.domain.ImportJob;
import com.aureus.ingestion.domain.ImportJobRepository;
import com.aureus.ledger.domain.Expense;
import com.aureus.ledger.domain.ExpenseRepository;
import com.aureus.ledger.domain.User;
import com.aureus.ledger.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportService {
    private final ImportJobRepository importJobRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final RevolutCsvParser revolutCsvParser;

    public ImportService(ImportJobRepository importJobRepository, ExpenseRepository expenseRepository,
                         UserRepository userRepository, RevolutCsvParser revolutCsvParser) {
        this.importJobRepository = importJobRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.revolutCsvParser = revolutCsvParser;
    }

    @Transactional
    public ImportResponse importCsv(ImportRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        // Reject the upload if this exact file name was already successfully imported
        checkDuplicate(userId, request.fileName());
        // Create the import job with a PROCESSING status
        ImportJob job = new ImportJob();
        job.setUser(user);
        job.setSource(request.source());
        job.setFileName(request.fileName());
        job.setStatus("PROCESSING");
        job = importJobRepository.save(job);
        try {
            // Parse the CSV content into candidate expenses
            List<Expense> parsed = revolutCsvParser.parse(request.csvContent(), user);
            // Separate new expenses from duplicates using the external_id fingerprint
            List<Expense> toSave = new ArrayList<>();
            int skipped = 0;
            for (Expense expense : parsed) {
                String extId = expense.getExternalId();
                if (extId != null && expenseRepository.existsByUserIdAndExternalId(userId, extId)) {
                    // This transaction already exists — skip it silently
                    skipped++;
                    continue;
                }
                expense.setImportJob(job);
                toSave.add(expense);
            }
            expenseRepository.saveAll(toSave);
            // Update the job status to DONE and record completion details
            job.setStatus("DONE");
            job.setTotalRows(parsed.size());
            job.setImportedRows(toSave.size());
            job.setSkippedRows(skipped);
            job.setFinishedAt(LocalDateTime.now());
            importJobRepository.save(job);
            return toResponse(job);
        } catch (Exception e) {
            // If anything fails, mark the job as FAILED and record the error
            job.setStatus("FAILED");
            job.setErrorDetail(e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            importJobRepository.save(job);
            return new ImportResponse(
                    job.getId(),
                    job.getStatus(),
                    0,
                    0,
                    0,
                    e.getMessage(),
                    job.getFileName(),
                    job.getCreatedAt()
            );
        }
    }

    private void checkDuplicate(Long userId, String fileName) {
        importJobRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(job -> "DONE".equals(job.getStatus()))
                .filter(job -> fileName.equals(job.getFileName()))
                .filter(job -> expenseRepository.existsByImportJobId(job.getId()))
                .findFirst()
                .ifPresent(job -> {
                    throw new IllegalStateException(
                            "El archivo '" + fileName + "' ya fue importado el " + job.getCreatedAt().toLocalDate()
                    );
                });
    }

    @Transactional
    public void deleteImportJob(Long jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Import job not found: " + jobId));
        expenseRepository.deleteByImportJobId(jobId);
        importJobRepository.delete(job);
    }

    @Transactional(readOnly = true)
    public List<ImportResponse> getImportJobs(Long userId) {
        return importJobRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteAllImportJobs(Long userId) {
        List<ImportJob> jobs = importJobRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (ImportJob job : jobs) {
            expenseRepository.deleteByImportJobId(job.getId());
        }
        importJobRepository.deleteAll(jobs);
    }

    // Maps an ImportJob entity to its API response record
    private ImportResponse toResponse(ImportJob job) {
        return new ImportResponse(
                job.getId(),
                job.getStatus(),
                job.getTotalRows() != null ? job.getTotalRows()    : 0,
                job.getImportedRows() != null ? job.getImportedRows() : 0,
                job.getSkippedRows() != null ? job.getSkippedRows()  : 0,
                job.getErrorDetail(),
                job.getFileName(),
                job.getCreatedAt()
        );
    }
}