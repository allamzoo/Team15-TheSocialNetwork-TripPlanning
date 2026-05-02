package com.team15.tripplanning.itineraryservice.dto;

public class DestinationRecommendationDTO {

    private Long destinationId;
    private String name;
    private String country;
    private String category;
    private Long score;

    public DestinationRecommendationDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DestinationRecommendationDTO dto = new DestinationRecommendationDTO();

        public Builder destinationId(Long v) { dto.destinationId = v; return this; }
        public Builder name(String v) { dto.name = v; return this; }
        public Builder country(String v) { dto.country = v; return this; }
        public Builder category(String v) { dto.category = v; return this; }
        public Builder score(Long v) { dto.score = v; return this; }

        public DestinationRecommendationDTO build() { return dto; }
    }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long v) { this.destinationId = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getCountry() { return country; }
    public void setCountry(String v) { this.country = v; }

    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }

    public Long getScore() { return score; }
    public void setScore(Long v) { this.score = v; }
}
