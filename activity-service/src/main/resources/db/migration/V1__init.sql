DO $$ BEGIN
    CREATE TYPE activitycategory AS ENUM (
        'SIGHTSEEING', 'ADVENTURE', 'DINING', 'CULTURAL', 'LEISURE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS activities (
    id BIGSERIAL PRIMARY KEY,
    itinerary_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category activitycategory NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    scheduled_time TIMESTAMP NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'
);
