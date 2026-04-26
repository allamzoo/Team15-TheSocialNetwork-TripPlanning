package com.team15.tripplanning.itineraryservice.dto;

import java.time.LocalDate;
import java.util.Map;

public class ItineraryDayRequestDTO {

    private LocalDate date;
    private String title;
    private String description;
    private Map<String, Object> metadata;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}