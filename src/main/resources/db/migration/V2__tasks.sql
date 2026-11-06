ALTER TABLE academic.courses
    ADD CONSTRAINT uk_course_id_period UNIQUE (id, period_id);

CREATE TABLE academic.tasks (
    id                   VARCHAR(36)  PRIMARY KEY,
    user_id              VARCHAR(36)  NOT NULL,
    title                VARCHAR(255) NOT NULL,
    description          TEXT         NOT NULL DEFAULT '',
    course_id            VARCHAR(36)  NOT NULL,
    period_id            VARCHAR(36)  NOT NULL,
    due_date             TIMESTAMPTZ  NOT NULL,
    status               VARCHAR(20)  NOT NULL
        CHECK (status IN ('todo', 'in-progress', 'in-review', 'done', 'cancelled')),
    priority             VARCHAR(20)  NOT NULL
        CHECK (priority IN ('low', 'medium', 'high', 'urgent')),
    type                 VARCHAR(20)  NOT NULL
        CHECK (type IN ('assignment', 'exam', 'reading', 'presentation', 'lab', 'quiz', 'essay', 'group-work')),
    estimated_time_hours INTEGER      NOT NULL CHECK (estimated_time_hours >= 0),
    actual_time_hours    INTEGER      CHECK (actual_time_hours >= 0),
    tags                 TEXT[]       NOT NULL DEFAULT '{}',
    group_members        TEXT[]       NOT NULL DEFAULT '{}',
    attachments          TEXT[]       NOT NULL DEFAULT '{}',
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    completed_at         TIMESTAMPTZ,

    -- A task belongs to a course and to the period of that course; deleting either is blocked while tasks exist.
    CONSTRAINT fk_task_course_period
        FOREIGN KEY (course_id, period_id) REFERENCES academic.courses (id, period_id)
        ON DELETE RESTRICT,
    CONSTRAINT check_task_group_members
        CHECK (type = 'group-work' OR cardinality(group_members) = 0)
);

CREATE INDEX idx_task_user_due_date ON academic.tasks (user_id, due_date);
CREATE INDEX idx_task_user_period ON academic.tasks (user_id, period_id);
CREATE INDEX idx_task_user_course ON academic.tasks (user_id, course_id);
CREATE INDEX idx_task_course ON academic.tasks (course_id);
