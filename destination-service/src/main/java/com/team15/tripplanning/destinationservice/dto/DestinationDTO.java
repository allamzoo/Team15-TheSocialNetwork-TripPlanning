package com.team15.tripplanning.destinationservice.dto;

import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;

public class DestinationDTO {
    private Long id;
    private String name;
    private String country;
    private DestinationCategory category;
    private Double rating;
    private DestinationStatus status;

    private DestinationDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DestinationDTO dto = new DestinationDTO();

        public Builder id(Long id) {
            dto.id = id;
            return this;
        }

        public Builder name(String name) {
            dto.name = name;
            return this;
        }

        public Builder country(String country) {
            dto.country = country;
            return this;
        }

        public Builder category(DestinationCategory category) {
            dto.category = category;
            return this;
        }

        public Builder rating(Double rating) {
            dto.rating = rating;
            return this;
        }

        public Builder status(DestinationStatus status) {
            dto.status = status;
            return this;
        }

        public DestinationDTO build() {
            return dto;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public DestinationCategory getCategory() { return category; }
    public void setCategory(DestinationCategory category) { this.category = category; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public DestinationStatus getStatus() { return status; }
    public void setStatus(DestinationStatus status) { this.status = status; }
}
