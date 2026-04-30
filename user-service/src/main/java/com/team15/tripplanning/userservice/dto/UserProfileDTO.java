package com.team15.tripplanning.userservice.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserProfileDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private Map<String, Object> preferences;
    private List<SavedDestinationProfileDTO> savedDestinations = new ArrayList<>();
    private Long totalSavedDestinations;

    public UserProfileDTO() {
    }

    public UserProfileDTO(Long userId, String name, String email, String phone,
                          Map<String, Object> preferences,
                          List<SavedDestinationProfileDTO> savedDestinations,
                          Long totalSavedDestinations) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferences = preferences;
        this.savedDestinations = savedDestinations != null ? savedDestinations : new ArrayList<>();
        this.totalSavedDestinations = totalSavedDestinations;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String name;
        private String email;
        private String phone;
        private Map<String, Object> preferences;
        private List<SavedDestinationProfileDTO> savedDestinations;
        private Long totalSavedDestinations;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder preferences(Map<String, Object> preferences) {
            this.preferences = preferences;
            return this;
        }

        public Builder savedDestinations(List<SavedDestinationProfileDTO> savedDestinations) {
            this.savedDestinations = savedDestinations;
            return this;
        }

        public Builder totalSavedDestinations(Long totalSavedDestinations) {
            this.totalSavedDestinations = totalSavedDestinations;
            return this;
        }

        public UserProfileDTO build() {
            return new UserProfileDTO(userId, name, email, phone, preferences, savedDestinations, totalSavedDestinations);
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public List<SavedDestinationProfileDTO> getSavedDestinations() {
        return savedDestinations;
    }

    public void setSavedDestinations(List<SavedDestinationProfileDTO> savedDestinations) {
        this.savedDestinations = savedDestinations != null ? savedDestinations : new ArrayList<>();
    }

    public Long getTotalSavedDestinations() {
        return totalSavedDestinations;
    }

    public void setTotalSavedDestinations(Long totalSavedDestinations) {
        this.totalSavedDestinations = totalSavedDestinations;
    }
}

