package com.team15.tripplanning.destinationservice.dto;

import com.team15.tripplanning.destinationservice.entity.DestinationReview;
import com.team15.tripplanning.destinationservice.entity.DestinationStatus;
import java.util.List;

public class DestinationReviewAlertDTO {
    private Long destinationId;
    private String destinationName;
    private DestinationStatus destinationStatus;
    private List<DestinationReview> lowRatedReviews;
    private Integer lowRatedCount;

    public DestinationReviewAlertDTO(Long destinationId, String destinationName,
                                     DestinationStatus destinationStatus,
                                     List<DestinationReview> lowRatedReviews) {
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.destinationStatus = destinationStatus;
        this.lowRatedReviews = lowRatedReviews;
        this.lowRatedCount = lowRatedReviews.size();
    }

    // Standard Getters and Setters
    public Long getDestinationId() { return destinationId; }
    public String getDestinationName() { return destinationName; }
    public DestinationStatus getDestinationStatus() { return destinationStatus; }
    public List<DestinationReview> getLowRatedReviews() { return lowRatedReviews; }
    public Integer getLowRatedCount() { return lowRatedCount; }
}