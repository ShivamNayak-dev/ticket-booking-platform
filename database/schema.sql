-- Ticket Booking Service - PostgreSQL schema
-- Run this once on an empty database called ticket_booking:
--   psql -U postgres -d ticket_booking -f database/schema.sql
-- (The backend also runs with spring.jpa.hibernate.ddl-auto=update, so it will
--  create/adjust these tables itself if you skip this file.)

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS events (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(150)  NOT NULL,
    description     VARCHAR(2000),
    category        VARCHAR(30)   NOT NULL,
    venue           VARCHAR(200)  NOT NULL,
    event_date      TIMESTAMP     NOT NULL,
    total_seats     INTEGER       NOT NULL,
    available_seats INTEGER       NOT NULL,
    price           NUMERIC(10,2) NOT NULL,
    created_by      BIGINT        NOT NULL REFERENCES users (id),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_events_category
        CHECK (category IN ('MUSIC', 'SPORTS', 'TECH', 'COMEDY', 'THEATRE', 'CONFERENCE', 'OTHER')),
    CONSTRAINT chk_events_total_seats CHECK (total_seats > 0),
    -- last line of defence against overbooking, even if application code has a bug
    CONSTRAINT chk_events_available_seats CHECK (available_seats >= 0 AND available_seats <= total_seats),
    CONSTRAINT chk_events_price CHECK (price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_events_category   ON events (category);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events (event_date);

CREATE TABLE IF NOT EXISTS bookings (
    id          BIGSERIAL PRIMARY KEY,
    reference   VARCHAR(20)   NOT NULL UNIQUE,
    user_id     BIGINT        NOT NULL REFERENCES users (id),
    event_id    BIGINT        NOT NULL REFERENCES events (id),
    quantity    INTEGER       NOT NULL,
    total_price NUMERIC(10,2) NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_bookings_quantity CHECK (quantity > 0),
    CONSTRAINT chk_bookings_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_bookings_user  ON bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_event ON bookings (event_id);

CREATE TABLE IF NOT EXISTS waitlist_entries (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    event_id   BIGINT      NOT NULL REFERENCES events (id),
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_waitlist_status CHECK (status IN ('WAITING', 'PROMOTED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_waitlist_event_status ON waitlist_entries (event_id, status);
CREATE INDEX IF NOT EXISTS idx_waitlist_user         ON waitlist_entries (user_id);
