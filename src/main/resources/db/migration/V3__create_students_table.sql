-- POSTGRES

--CREATE TABLE IF NOT EXISTS academic.students (
--    id VARCHAR(36) PRIMARY KEY,
--    name VARCHAR(255),
--    email VARCHAR(255) NOT NULL,
--    avatar TEXT,
----    provider VARCHAR(50) NOT NULL,
----    provider_id VARCHAR(255) NOT NULL,
----    student_id VARCHAR(50),
----    access_token TEXT NOT NULL,
--    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
--    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
--);
--
--CREATE INDEX idx_student_email
--    ON academic.students (email);


-- SQL SERVER

-- Create students table
CREATE TABLE academic.students (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    avatar NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create index
CREATE INDEX idx_student_email
    ON academic.students (email);