package com.aureus.ingestion.domain;

import com.aureus.ledger.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_jobs")
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(length = 255)
    private String fileName;

    private Integer totalRows;

    private Integer importedRows;

    @Column(columnDefinition = "TEXT")
    private String errorDetail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime finishedAt;

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

    public Integer getImportedRows() { return importedRows; }
    public void setImportedRows(Integer importedRows) { this.importedRows = importedRows; }

    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}