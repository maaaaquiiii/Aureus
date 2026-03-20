CREATE TABLE IF NOT EXISTS llm_analyses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    period VARCHAR(7) NOT NULL,
    prompt_used TEXT NOT NULL,
    analysis TEXT NOT NULL,
    model_used VARCHAR(50) NOT NULL DEFAULT 'gpt-4.1',
    tokens_used INT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, period)
);