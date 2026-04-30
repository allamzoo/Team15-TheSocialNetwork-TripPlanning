package com.team15.tripplanning.activityservice.dto;

public class NearbyActivityDTO {
    private Long activityId;
    private String name;
    private String category;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;

    private NearbyActivityDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final NearbyActivityDTO dto = new NearbyActivityDTO();

        public Builder activityId(Long activityId) { dto.activityId = activityId; return this; }
        public Builder name(String name) { dto.name = name; return this; }
        public Builder category(String category) { dto.category = category; return this; }
        public Builder latitude(Double latitude) { dto.latitude = latitude; return this; }
        public Builder longitude(Double longitude) { dto.longitude = longitude; return this; }
        public Builder distanceKm(Double distanceKm) { dto.distanceKm = distanceKm; return this; }

        public NearbyActivityDTO build() { return dto; }
    }

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
