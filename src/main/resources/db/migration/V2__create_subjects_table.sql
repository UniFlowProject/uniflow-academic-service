-- POSRGRES

--CREATE TABLE IF NOT EXISTS academic.subjects (
--    id VARCHAR(36) PRIMARY KEY,
--    name VARCHAR(255) NOT NULL,
--    code VARCHAR(50) NOT NULL,
--    professor VARCHAR(255),
--    credits INTEGER,
--    color VARCHAR(20),
--    period_id VARCHAR(36) NOT NULL,
--    student_id VARCHAR(36) NOT NULL,
--    description TEXT,
--    schedule TEXT,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
--CREATE UNIQUE INDEX IF NOT EXISTS uk_subject_period_code
--    ON academic.subjects (student_id, period_id, LOWER(code));
--
--CREATE INDEX IF NOT EXISTS idx_subject_student
--    ON academic.subjects (student_id);
--
--CREATE INDEX IF NOT EXISTS idx_subject_period
--    ON academic.subjects (period_id);
--
--CREATE INDEX IF NOT EXISTS idx_subject_professor
--    ON academic.subjects (professor);
--
--ALTER TABLE academic.subjects
--    ADD CONSTRAINT fk_subject_period
--        FOREIGN KEY (period_id) REFERENCES academic.periods (id)
--        ON DELETE CASCADE;


-- SQL SERVER

-- Create subjects table
CREATE TABLE academic.subjects (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    professor VARCHAR(255),
    credits INT,
    color VARCHAR(20),
    period_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    description NVARCHAR(MAX),
    schedule NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique index with LOWER() for case-insensitive search
CREATE UNIQUE INDEX uk_subject_period_code
    ON academic.subjects (student_id, period_id, code)
    WHERE code IS NOT NULL;

-- Create regular indexes
CREATE INDEX idx_subject_student
    ON academic.subjects (student_id);

CREATE INDEX idx_subject_period
    ON academic.subjects (period_id);

CREATE INDEX idx_subject_professor
    ON academic.subjects (professor);

-- Add foreign key constraint
ALTER TABLE academic.subjects
    ADD CONSTRAINT fk_subject_period
        FOREIGN KEY (period_id) REFERENCES academic.periods (id)
        ON DELETE CASCADE;