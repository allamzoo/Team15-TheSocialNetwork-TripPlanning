package com.team15.tripplanning.userservice.dto;

public class TopTravelerDTO {
    private Long userId;
    private String name;
    private Double totalSpent;
    private Long tripCount;

    public TopTravelerDTO() {
    }

    public TopTravelerDTO(Long userId, String name, Double totalSpent, Long tripCount) {
        this.userId = userId;
        this.name = name;
        this.totalSpent = totalSpent;
        this.tripCount = tripCount;
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

    public Double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public Long getTripCount() {
        return tripCount;
    }

    public void setTripCount(Long tripCount) {
        this.tripCount = tripCount;
    }
}

