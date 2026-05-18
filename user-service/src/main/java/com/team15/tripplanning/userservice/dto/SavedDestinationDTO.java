package com.team15.tripplanning.userservice.dto;

import java.util.Map;

public class SavedDestinationDTO {

    private Long id;
    private Long destinationId;
    private String label;
    private String destinationName;
    private String country;
    private Double latitude;
    private Double longitude;
    private Boolean isDefault;
    private Map<String, Object> metadata;

    private SavedDestinationDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SavedDestinationDTO dto = new SavedDestinationDTO();

        public Builder id(Long id)                          { dto.id = id; return this; }
        public Builder destinationId(Long v)                { dto.destinationId = v; return this; }
        public Builder label(String v)                      { dto.label = v; return this; }
        public Builder destinationName(String v)            { dto.destinationName = v; return this; }
        public Builder country(String v)                    { dto.country = v; return this; }
        public Builder latitude(Double v)                   { dto.latitude = v; return this; }
        public Builder longitude(Double v)                  { dto.longitude = v; return this; }
        public Builder isDefault(Boolean v)                 { dto.isDefault = v; return this; }
        public Builder metadata(Map<String, Object> v)      { dto.metadata = v; return this; }

        public SavedDestinationDTO build() { return dto; }
    }

    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public Long getDestinationId()                         { return destinationId; }
    public void setDestinationId(Long v)                   { this.destinationId = v; }

    public String getLabel()                               { return label; }
    public void setLabel(String v)                         { this.label = v; }

    public String getDestinationName()                     { return destinationName; }
    public void setDestinationName(String v)               { this.destinationName = v; }

    public String getCountry()                             { return country; }
    public void setCountry(String v)                       { this.country = v; }

    public Double getLatitude()                            { return latitude; }
    public void setLatitude(Double v)                      { this.latitude = v; }

    public Double getLongitude()                           { return longitude; }
    public void setLongitude(Double v)                     { this.longitude = v; }

    public Boolean getIsDefault()                          { return isDefault; }
    public void setIsDefault(Boolean v)                    { this.isDefault = v; }

    public Map<String, Object> getMetadata()               { return metadata; }
    public void setMetadata(Map<String, Object> v)         { this.metadata = v; }
}
