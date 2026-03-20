CREATE TABLE IF NOT EXISTS expenses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    import_job_id BIGINT REFERENCES import_jobs(id),
    category_id BIGINT NOT NULL REFERENCES categories(id),
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(500),
    incurred_on DATE NOT NULL,
    source VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_expenses_user_month ON expenses(user_id, incurred_on);
CREATE INDEX IF NOT EXISTS idx_expenses_user_category ON expenses(user_id, category_id);