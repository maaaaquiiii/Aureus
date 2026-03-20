package com.aureus.llm.domain;

import com.aureus.ledger.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_analyses", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period"}))
public class LlmAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String promptUsed;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String analysis;

    @Column(nullable = false, length = 50)
    private String modelUsed = "gpt-4.1";

    private Integer tokensUsed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getPromptUsed() { return promptUsed; }
    public void setPromptUsed(String promptUsed) { this.promptUsed = promptUsed; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}