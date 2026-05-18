package com.team15.tripplanning.bookingservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the bookings.status column can accept all required status values
 * including PLANNED, COMPLETED, IN_PROGRESS, REFUNDED which are used by test helpers.
 */
@Component
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);
    private final JdbcTemplate jdbc;

    public DatabaseMigrationConfig(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateBookingStatusColumn() {
        try {
            // Check if the bookingstatus type exists (PostgreSQL named enum)
            Integer typeExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_type WHERE typname = 'bookingstatus'",
                Integer.class
            );

            if (typeExists != null && typeExists > 0) {
                // Check if the column is of enum type
                Integer colIsEnum = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns c " +
                    "JOIN pg_type t ON c.udt_name = t.typname " +
                    "WHERE c.table_name = 'bookings' AND c.column_name = 'status' " +
                    "AND t.typtype = 'e'",
                    Integer.class
                );

                if (colIsEnum != null && colIsEnum > 0) {
                    // Column is still a named enum — migrate to VARCHAR
                    log.info("Migrating bookings.status from named enum to VARCHAR");
                    jdbc.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS status_varchar VARCHAR(50) DEFAULT 'PENDING'");
                    jdbc.execute("UPDATE bookings SET status_varchar = status::TEXT WHERE status_varchar IS NULL OR status_varchar = 'PENDING'");
                    jdbc.execute("ALTER TABLE bookings ALTER COLUMN status_varchar SET NOT NULL");
                    jdbc.execute("ALTER TABLE bookings DROP COLUMN status");
                    jdbc.execute("ALTER TABLE bookings RENAME COLUMN status_varchar TO status");
                    log.info("Successfully migrated bookings.status to VARCHAR");
                }
            }

            // Also ensure start_date and end_date columns exist
            jdbc.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS start_date DATE");
            jdbc.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS end_date DATE");

            // M3 settlements table (saga anchor)
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS settlements (
                    id BIGSERIAL PRIMARY KEY,
                    itinerary_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    amount NUMERIC(10,2) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    settled_at TIMESTAMP NULL,
                    failure_reason VARCHAR(255) NULL
                )
                """);
            jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_settlements_itinerary_id ON settlements (itinerary_id)");

        } catch (Exception e) {
            log.warn("Database migration for bookings.status skipped or partially failed: {}", e.getMessage());
        }
    }
}
