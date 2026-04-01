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
    public ImportResponse importCsv(ImportRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.userId()));
        checkDuplicate(request.userId(), request.fileName());
        // Create the import job with a PROCESSING status
        ImportJob job = new ImportJob();
        job.setUser(user);
        job.setSource(request.source());
        job.setFileName(request.fileName());
        job.setStatus("PROCESSING");
        job = importJobRepository.save(job);

        try {
            // Parse the CSV content
            List<Expense> expenses = revolutCsvParser.parse(request.csvContent(), user);
            job.setTotalRows(expenses.size());
            // Link each expense to the import job and save it to the database
            for (Expense expense : expenses) {
                expense.setImportJob(job);
                expenseRepository.save(expense);
            }
            // Update the job status to DONE and record completion details
            job.setStatus("DONE");
            job.setImportedRows(expenses.size());
            job.setFinishedAt(LocalDateTime.now());
            importJobRepository.save(job);
            return new ImportResponse(
                    job.getId(),
                    job.getStatus(),
                    job.getTotalRows(),
                    job.getImportedRows(),
                    null,
                    job.getFileName(),
                    job.getCreatedAt()
            );

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
                    e.getMessage(),
                    job.getFileName(),
                    job.getCreatedAt()
            );
        }
    }

    @Transactional(readOnly = true)
    private void checkDuplicate(Long userId, String fileName) {
        importJobRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(job -> "DONE".equals(job.getStatus()))
                .filter(job -> fileName.equals(job.getFileName()))
                .findFirst()
                .ifPresent(job -> {
                    throw new RuntimeException(
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
                .map(job -> new ImportResponse(
                        job.getId(),
                        job.getStatus(),
                        job.getTotalRows(),
                        job.getImportedRows(),
                        job.getErrorDetail(),
                        job.getFileName(),
                        job.getCreatedAt()
                ))
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
}