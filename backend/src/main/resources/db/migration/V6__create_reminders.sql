CREATE TABLE reminders (
    id           UUID PRIMARY KEY,
    vehicle_id   UUID         NOT NULL,
    title        VARCHAR(120) NOT NULL,
    description  TEXT,
    due_date     DATE,
    due_mileage  INTEGER,
    status       VARCHAR(20)  NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_reminders_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE,
    CONSTRAINT ck_reminders_due_mileage CHECK (due_mileage IS NULL OR due_mileage >= 0),
    -- A reminder without any due condition can never trigger; reject it at the database level too.
    CONSTRAINT ck_reminders_has_due_condition CHECK (due_date IS NOT NULL OR due_mileage IS NOT NULL)
);

CREATE INDEX idx_reminders_vehicle_status ON reminders (vehicle_id, status);
