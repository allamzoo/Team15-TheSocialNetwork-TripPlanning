package com.team15.tripplanning.destinationservice.repository;

import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ItineraryLookupRepository {
    private final JdbcTemplate jdbcTemplate;

    public ItineraryLookupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Map<String, Object>> findItineraryById(Long itineraryId) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT id, destination_id, status FROM itineraries WHERE id = ?",
                    itineraryId
            );
            return Optional.of(row);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
