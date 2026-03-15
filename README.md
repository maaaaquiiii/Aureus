```mermeid

erDiagram

    users {

        BIGSERIAL id PK

        VARCHAR_255 email "NOT NULL UNIQUE"

        VARCHAR_255 name "NOT NULL"

        VARCHAR_3 currency "DEFAULT EUR"

        TIMESTAMP created_at "NOT NULL"

    }

    import_jobs {

        BIGSERIAL id PK

        BIGINT user_id FK

        VARCHAR_100 source "NOT NULL"

        VARCHAR_20 status "DEFAULT PENDING"

        VARCHAR_255 file_name

        INT total_rows

        INT imported_rows

        TEXT error_detail

        TIMESTAMP created_at "NOT NULL"

        TIMESTAMP finished_at

    }

    expenses {

        BIGSERIAL id PK

        BIGINT user_id FK

        BIGINT import_job_id FK

        VARCHAR_100 category "NOT NULL"

        DECIMAL_12_2 amount "NOT NULL"

        VARCHAR_3 currency "NOT NULL"

        VARCHAR_500 description

        DATE incurred_on "NOT NULL"

        VARCHAR_100 source

        TIMESTAMP created_at "NOT NULL"

    }

    llm_analyses {

        BIGSERIAL id PK

        BIGINT user_id FK

        VARCHAR_7 period "NOT NULL ej:2025-03"

        TEXT prompt_used "NOT NULL"

        TEXT analysis "NOT NULL"

        VARCHAR_50 model_used "NOT NULL"

        INT tokens_used

        TIMESTAMP created_at "NOT NULL"

    }

    users ||--o{ import_jobs : "tiene"

    users ||--o{ expenses : "tiene"

    users ||--o{ llm_analyses : "tiene"

    import_jobs ||--o{ expenses : "genera"

```

