CREATE TABLE maintenance_records (
    id                   UUID PRIMARY KEY,
    vehicle_id           UUID          NOT NULL,
    category             VARCHAR(30)   NOT NULL,
    title                VARCHAR(120)  NOT NULL,
    description          TEXT,
    service_date         DATE          NOT NULL,
    mileage_at_service   INTEGER       NOT NULL,
    cost                 NUMERIC(12, 2) NOT NULL,
    workshop             VARCHAR(120),
    next_service_date    DATE,
    next_service_mileage INTEGER,
    created_at           TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_maintenance_records_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE,
    CONSTRAINT ck_maintenance_records_mileage CHECK (mileage_at_service >= 0),
    CONSTRAINT ck_maintenance_records_cost CHECK (cost >= 0),
    CONSTRAINT ck_maintenance_records_next_mileage CHECK (next_service_mileage IS NULL OR next_service_mileage >= 0)
);

CREATE INDEX idx_maintenance_records_vehicle_date ON maintenance_records (vehicle_id, service_date DESC);
