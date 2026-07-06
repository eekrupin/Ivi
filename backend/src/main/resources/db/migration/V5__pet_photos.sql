CREATE TABLE pet_photos (
    pet_id UUID PRIMARY KEY REFERENCES pets(id) ON DELETE CASCADE,
    revision VARCHAR(160) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    data BYTEA NOT NULL,
    size_bytes INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
