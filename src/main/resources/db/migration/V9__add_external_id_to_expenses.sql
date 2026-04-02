ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS external_id VARCHAR(64);

-- Partial unique index: only enforces uniqueness when external_id is present,
-- which allows existing rows (without external_id) to coexist without errors
CREATE UNIQUE INDEX IF NOT EXISTS uq_expenses_user_external_id
    ON expenses (user_id, external_id)
    WHERE external_id IS NOT NULL;