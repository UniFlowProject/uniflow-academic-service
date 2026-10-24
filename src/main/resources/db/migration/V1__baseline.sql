CREATE SCHEMA IF NOT EXISTS academic;

CREATE TABLE academic.periods (
    id         VARCHAR(36)  PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(50)  NOT NULL CHECK (type IN ('first-semester', 'second-semester', 'summer', 'special')),
    year       INTEGER      NOT NULL CHECK (year >= 1900 AND year <= 2100),
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_period_dates CHECK (start_date <= end_date)
);

CREATE INDEX idx_period_user_id ON academic.periods (user_id);
CREATE INDEX idx_period_is_active ON academic.periods (is_active);
CREATE INDEX idx_period_year ON academic.periods (year);
CREATE INDEX idx_period_type ON academic.periods (type);
CREATE INDEX idx_period_user_active ON academic.periods (user_id, is_active);
CREATE INDEX idx_period_start_date ON academic.periods (start_date);
CREATE INDEX idx_period_end_date ON academic.periods (end_date);

CREATE UNIQUE INDEX uk_period_one_active_per_user
    ON academic.periods (user_id)
    WHERE is_active;

CREATE TABLE academic.courses (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    professor   VARCHAR(255),
    credits     INTEGER,
    color       VARCHAR(20),
    period_id   VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    description TEXT,
    schedule    TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_course_period
        FOREIGN KEY (period_id) REFERENCES academic.periods (id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_course_user_period_code
    ON academic.courses (user_id, period_id, LOWER(code));

CREATE INDEX idx_course_user ON academic.courses (user_id);
CREATE INDEX idx_course_period ON academic.courses (period_id);
CREATE INDEX idx_course_professor ON academic.courses (professor);
