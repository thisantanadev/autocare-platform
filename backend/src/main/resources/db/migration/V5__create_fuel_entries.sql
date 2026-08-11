CREATE TABLE fuel_entries (
    id              UUID PRIMARY KEY,
    vehicle_id      UUID           NOT NULL,
    refuel_date     DATE           NOT NULL,
    odometer        INTEGER        NOT NULL,
    liters          NUMERIC(8, 3)  NOT NULL,
    total_cost      NUMERIC(12, 2) NOT NULL,
    price_per_liter NUMERIC(8, 3)  NOT NULL,
    full_tank       BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_fuel_entries_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE,
    CONSTRAINT ck_fuel_entries_odometer CHECK (odometer >= 0),
    CONSTRAINT ck_fuel_entries_liters CHECK (liters > 0),
    CONSTRAINT ck_fuel_entries_total_cost CHECK (total_cost > 0),
    CONSTRAINT ck_fuel_entries_price_per_liter CHECK (price_per_liter > 0)
);

CREATE INDEX idx_fuel_entries_vehicle_date ON fuel_entries (vehicle_id, refuel_date DESC);
CREATE INDEX idx_fuel_entries_vehicle_odometer ON fuel_entries (vehicle_id, odometer);
