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
                    null
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
                    e.getMessage()
            );
        }
    }
}