package com.aureus.ingestion.service;

import com.aureus.ingestion.api.ImportRequest;
import com.aureus.ingestion.api.ImportResponse;
import com.aureus.ingestion.domain.ImportJob;
import com.aureus.ingestion.domain.ImportJobRepository;
import com.aureus.ledger.domain.Category;
import com.aureus.ledger.domain.Expense;
import com.aureus.ledger.domain.ExpenseRepository;
import com.aureus.ledger.domain.User;
import com.aureus.ledger.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {
    @Mock private ImportJobRepository importJobRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;
    @Mock private RevolutCsvParser revolutCsvParser;
    @InjectMocks
    private ImportService importService;

    @Test
    void importCsv_withValidCsv_returnsSuccessResponse() {
        User user = buildUser(1L);
        ImportJob savedJob = buildJob(10L, user, "DONE", "test.csv");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(importJobRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(importJobRepository.save(any(ImportJob.class))).thenReturn(savedJob);
        Expense expense = buildExpense(user, "ext-001");
        when(revolutCsvParser.parse(any(), any())).thenReturn(List.of(expense));
        when(expenseRepository.existsByUserIdAndExternalId(1L, "ext-001")).thenReturn(false);
        when(expenseRepository.saveAll(any())).thenReturn(List.of(expense));
        ImportRequest request = new ImportRequest("revolut", "csv-content", "test.csv");
        ImportResponse response = importService.importCsv(request, 1L);
        assertThat(response.status()).isEqualTo("DONE");
        verify(expenseRepository).saveAll(any());
    }

    @Test
    void importCsv_whenAllRowsDuplicated_skipsAllAndReturnsZeroImported() {
        User user = buildUser(1L);
        ImportJob savedJob = buildJob(10L, user, "DONE", "test.csv");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(importJobRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(importJobRepository.save(any(ImportJob.class))).thenReturn(savedJob);
        Expense expense = buildExpense(user, "ext-001");
        when(revolutCsvParser.parse(any(), any())).thenReturn(List.of(expense));
        when(expenseRepository.existsByUserIdAndExternalId(1L, "ext-001")).thenReturn(true);
        when(expenseRepository.saveAll(any())).thenReturn(List.of());
        ImportRequest request = new ImportRequest("revolut", "csv-content", "test.csv");
        importService.importCsv(request, 1L);
        verify(expenseRepository).saveAll(argThat(list -> ((List<?>) list).isEmpty()));
    }

    @Test
    void importCsv_whenParserThrows_returnsFailedResponse() {
        User user = buildUser(1L);
        ImportJob savedJob = buildJob(10L, user, "FAILED", "test.csv");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(importJobRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(importJobRepository.save(any(ImportJob.class))).thenReturn(savedJob);
        when(revolutCsvParser.parse(any(), any())).thenThrow(new RuntimeException("CSV malformado"));
        ImportRequest request = new ImportRequest("revolut", "bad-csv", "test.csv");
        ImportResponse response = importService.importCsv(request, 1L);
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.errorDetail()).isEqualTo("CSV malformado");
    }

    @Test
    void importCsv_whenUserNotFound_throwsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        ImportRequest request = new ImportRequest("revolut", "csv", "test.csv");
        assertThatThrownBy(() -> importService.importCsv(request, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void importCsv_whenFileAlreadyImportedAndExpensesExist_throwsIllegalStateException() {
        User user = buildUser(1L);
        ImportJob existingJob = buildJob(5L, user, "DONE", "test.csv");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(importJobRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(existingJob));
        when(expenseRepository.existsByImportJobId(5L)).thenReturn(true);
        ImportRequest request = new ImportRequest("revolut", "csv", "test.csv");
        assertThatThrownBy(() -> importService.importCsv(request, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("test.csv");
    }

    @Test
    void importCsv_whenFileExistsButExpensesDeleted_allowsReimport() {
        User user = buildUser(1L);
        ImportJob existingJob = buildJob(5L, user, "DONE", "test.csv");
        ImportJob newJob = buildJob(10L, user, "DONE", "test.csv");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(importJobRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(existingJob));
        when(expenseRepository.existsByImportJobId(5L)).thenReturn(false);
        when(importJobRepository.save(any(ImportJob.class))).thenReturn(newJob);
        when(revolutCsvParser.parse(any(), any())).thenReturn(List.of());
        when(expenseRepository.saveAll(any())).thenReturn(List.of());
        ImportRequest request = new ImportRequest("revolut", "csv", "test.csv");
        ImportResponse response = importService.importCsv(request, 1L);
        assertThat(response).isNotNull();
    }

    @Test
    void importCsv_whenExistingJobIsNotDone_allowsImport() {
        User user = buildUser(1L);
        ImportJob failedJob = buildJob(5L, user, "FAILED", "test.csv");
        ImportJob newJob = buildJob(10L, user, "DONE", "test.csv");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(importJobRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(failedJob));
        when(importJobRepository.save(any(ImportJob.class))).thenReturn(newJob);
        when(revolutCsvParser.parse(any(), any())).thenReturn(List.of());
        when(expenseRepository.saveAll(any())).thenReturn(List.of());
        ImportRequest request = new ImportRequest("revolut", "csv", "test.csv");
        ImportResponse response = importService.importCsv(request, 1L);
        assertThat(response).isNotNull();
        verify(expenseRepository, never()).existsByImportJobId(any());
    }

    @Test
    void deleteImportJob_whenExists_deletesExpensesAndJob() {
        User user = buildUser(1L);
        ImportJob job = buildJob(10L, user, "DONE", "test.csv");
        when(importJobRepository.findById(10L)).thenReturn(Optional.of(job));
        importService.deleteImportJob(10L);
        verify(expenseRepository).deleteByImportJobId(10L);
        verify(importJobRepository).delete(job);
    }

    @Test
    void deleteImportJob_whenNotFound_throwsRuntimeException() {
        when(importJobRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> importService.deleteImportJob(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Import job not found");
    }

    @Test
    void getImportJobs_returnsMappedList() {
        User user = buildUser(1L);
        ImportJob job = buildJob(1L, user, "DONE", "test.csv");
        when(importJobRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(job));
        List<ImportResponse> result = importService.getImportJobs(1L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).fileName()).isEqualTo("test.csv");
        assertThat(result.get(0).status()).isEqualTo("DONE");
    }

    @Test
    void getImportJobs_whenNone_returnsEmptyList() {
        when(importJobRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());
        List<ImportResponse> result = importService.getImportJobs(1L);
        assertThat(result).isEmpty();
    }

    @Test
    void deleteAllImportJobs_deletesExpensesAndAllJobs() {
        User user = buildUser(1L);
        ImportJob job1 = buildJob(1L, user, "DONE", "file1.csv");
        ImportJob job2 = buildJob(2L, user, "DONE", "file2.csv");
        when(importJobRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(job1, job2));
        importService.deleteAllImportJobs(1L);
        verify(expenseRepository).deleteByImportJobId(1L);
        verify(expenseRepository).deleteByImportJobId(2L);
        verify(importJobRepository).deleteAll(List.of(job1, job2));
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setEmail("test@email.com");
        user.setName("Test");
        user.setCurrency("EUR");
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {}
        return user;
    }

    private ImportJob buildJob(Long id, User user, String status, String fileName) {
        ImportJob job = new ImportJob();
        job.setUser(user);
        job.setSource("revolut");
        job.setStatus(status);
        job.setFileName(fileName);
        job.setTotalRows(1);
        job.setImportedRows(1);
        job.setSkippedRows(0);
        job.setFinishedAt(LocalDateTime.now());
        try {
            var idField = ImportJob.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(job, id);
            var createdAtField = ImportJob.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(job, LocalDateTime.now());
        } catch (Exception ignored) {}
        return job;
    }

    private Expense buildExpense(User user, String externalId) {
        Category cat = new Category();
        cat.setName("Other");
        cat.setColor("#64748b");

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(cat);
        expense.setAmount(new BigDecimal("25.00"));
        expense.setCurrency("EUR");
        expense.setIncurredOn(LocalDate.now());
        expense.setDescription("Test expense");
        expense.setSource("revolut");
        expense.setExternalId(externalId);
        return expense;
    }
}