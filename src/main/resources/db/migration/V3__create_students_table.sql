CREATE TABLE IF NOT EXISTS academic.students (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    student_id VARCHAR(50),
    avatar TEXT,
    access_token TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE academic.students
    ADD CONSTRAINT IF NOT EXISTS uk_student_provider UNIQUE (provider, provider_id);

ALTER TABLE academic.students
    ADD CONSTRAINT IF NOT EXISTS uk_student_student_id UNIQUE (student_id);

CREATE INDEX IF NOT EXISTS idx_student_email
    ON academic.students (email);
