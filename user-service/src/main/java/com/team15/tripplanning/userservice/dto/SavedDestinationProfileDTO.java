package com.team15.tripplanning.userservice.dto;

import java.util.Map;

public class SavedDestinationProfileDTO {
    private String label;
    private String destinationName;
    private String country;
    private Double latitude;
    private Double longitude;
    private Boolean isDefault;
    private Map<String, Object> metadata;

    public SavedDestinationProfileDTO() {
    }

    public SavedDestinationProfileDTO(String label, String destinationName, String country,
                                      Double latitude, Double longitude, Boolean isDefault,
                                      Map<String, Object> metadata) {
        this.label = label;
        this.destinationName = destinationName;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isDefault = isDefault;
        this.metadata = metadata;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

