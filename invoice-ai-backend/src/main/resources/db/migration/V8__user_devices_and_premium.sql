-- Enregistrement appareils + statut Premium serveur (PRD §7.1)

ALTER TABLE users
    ADD COLUMN premium BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN premium_synced_at TIMESTAMP;

CREATE TABLE user_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(128) NOT NULL,
    device_name VARCHAR(255),
    platform VARCHAR(32),
    registered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_user_devices_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX idx_user_devices_user_active ON user_devices(user_id) WHERE is_active = TRUE;
