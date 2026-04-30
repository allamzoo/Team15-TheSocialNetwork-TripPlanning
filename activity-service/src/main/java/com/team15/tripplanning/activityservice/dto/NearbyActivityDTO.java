package com.team15.tripplanning.activityservice.dto;

public class NearbyActivityDTO {
    private Long activityId;
    private String name;
    private String category;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;

    public NearbyActivityDTO(Long activityId, String name, String category,
                             Double latitude, Double longitude, Double distanceKm) {
        this.activityId = activityId;
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long activityId;
        private String name;
        private String category;
        private Double latitude;
        private Double longitude;
        private Double distanceKm;

        public Builder activityId(Long activityId) {
            this.activityId = activityId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public NearbyActivityDTO build() {
            return new NearbyActivityDTO(activityId, name, category, latitude, longitude, distanceKm);
        }
    }

    // Getters and setters (required for JSON serialization)
    // Getters and setters
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
}