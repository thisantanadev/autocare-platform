CREATE TABLE vehicles (
    id                 UUID PRIMARY KEY,
    user_id            UUID         NOT NULL,
    brand              VARCHAR(60)  NOT NULL,
    model              VARCHAR(80)  NOT NULL,
    manufacturing_year INTEGER      NOT NULL,
    model_year         INTEGER,
    license_plate      VARCHAR(8),
    current_mileage    INTEGER      NOT NULL DEFAULT 0,
    fuel_type          VARCHAR(20)  NOT NULL,
    nickname           VARCHAR(60),
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_vehicles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_vehicles_current_mileage CHECK (current_mileage >= 0),
    CONSTRAINT ck_vehicles_manufacturing_year CHECK (manufacturing_year >= 1900)
);

CREATE INDEX idx_vehicles_user_id ON vehicles (user_id);

-- The same physical vehicle (plate) must not be registered twice by one user.
CREATE UNIQUE INDEX uk_vehicles_user_plate ON vehicles (user_id, license_plate)
    WHERE license_plate IS NOT NULL;
