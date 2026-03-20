CREATE TABLE IF NOT EXISTS budgets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    category_id BIGINT NOT NULL REFERENCES categories(id),
    period VARCHAR(7) NOT NULL,
    limit_amount DECIMAL(12, 2) NOT NULL,
    UNIQUE (user_id, category_id, period)
);