CREATE TABLE event_types (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(32) NOT NULL,
    default_duration_days INTEGER,
    is_active BOOLEAN NOT NULL,
    color_argb INTEGER,
    icon_key VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL
);

CREATE TABLE pet_events (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    event_type_id UUID NOT NULL REFERENCES event_types(id) ON DELETE RESTRICT,
    event_date DATE NOT NULL,
    due_date DATE,
    comment TEXT,
    notifications_enabled BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL
);

CREATE TABLE weight_entries (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    weight_grams INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL
);

CREATE INDEX idx_event_types_pet_id ON event_types(pet_id);
CREATE INDEX idx_event_types_pet_id_deleted_at ON event_types(pet_id, deleted_at);

CREATE INDEX idx_pet_events_pet_id ON pet_events(pet_id);
CREATE INDEX idx_pet_events_event_type_id ON pet_events(event_type_id);
CREATE INDEX idx_pet_events_pet_id_deleted_at ON pet_events(pet_id, deleted_at);

CREATE INDEX idx_weight_entries_pet_id ON weight_entries(pet_id);
CREATE INDEX idx_weight_entries_pet_id_deleted_at ON weight_entries(pet_id, deleted_at);
