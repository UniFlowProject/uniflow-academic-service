-- POSTGRES

---- Create schema
--CREATE SCHEMA IF NOT EXISTS academic;
--
---- Create periods table
--CREATE TABLE academic.periods (
--    id VARCHAR(36) PRIMARY KEY,
--    name VARCHAR(255) NOT NULL,
--    type VARCHAR(50) NOT NULL CHECK (type IN ('first-semester', 'second-semester', 'summer', 'special')),
--    year INTEGER NOT NULL CHECK (year >= 1900 AND year <= 2100),
--    start_date DATE NOT NULL,
--    end_date DATE NOT NULL,
--    student_id VARCHAR(36) NOT NULL,
--    is_active BOOLEAN NOT NULL DEFAULT FALSE,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--
--    CONSTRAINT check_dates CHECK (start_date < end_date)
--);
--
---- Create indexes
--CREATE INDEX idx_period_student_id ON academic.periods(student_id);
--CREATE INDEX idx_period_is_active ON academic.periods(is_active);
--CREATE INDEX idx_period_year ON academic.periods(year);
--CREATE INDEX idx_period_type ON academic.periods(type);
--CREATE INDEX idx_period_student_active ON academic.periods(student_id, is_active);
--CREATE INDEX idx_period_start_date ON academic.periods(start_date);
--CREATE INDEX idx_period_end_date ON academic.periods(end_date);


-- SQL SERVER

-- Create schema
IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = 'academic')
BEGIN
    EXEC sp_executesql N'CREATE SCHEMA academic'
END;

-- Create periods table
CREATE TABLE academic.periods (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('first-semester', 'second-semester', 'summer', 'special')),
    year INT NOT NULL CHECK (year >= 1900 AND year <= 2100),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    is_active BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT check_dates CHECK (start_date < end_date)
);

-- Create indexes
CREATE INDEX idx_period_student_id ON academic.periods(student_id);
CREATE INDEX idx_period_is_active ON academic.periods(is_active);
CREATE INDEX idx_period_year ON academic.periods(year);
CREATE INDEX idx_period_type ON academic.periods(type);
CREATE INDEX idx_period_student_active ON academic.periods(student_id, is_active);
CREATE INDEX idx_period_start_date ON academic.periods(start_date);
CREATE INDEX idx_period_end_date ON academic.periods(end_date);