package com.team15.tripplanning.destinationservice.dto;

import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
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
        this.lowRatedCount = lowRatedReviews == null ? 0 : lowRatedReviews.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long destinationId;
        private String destinationName;
        private DestinationStatus destinationStatus;
        private List<DestinationReview> lowRatedReviews;

        public Builder destinationId(Long destinationId) {
            this.destinationId = destinationId;
            return this;
        }

        public Builder destinationName(String destinationName) {
            this.destinationName = destinationName;
            return this;
        }

        public Builder destinationStatus(DestinationStatus destinationStatus) {
            this.destinationStatus = destinationStatus;
            return this;
        }

        public Builder lowRatedReviews(List<DestinationReview> lowRatedReviews) {
            this.lowRatedReviews = lowRatedReviews;
            return this;
        }

        public DestinationReviewAlertDTO build() {
            return new DestinationReviewAlertDTO(
                    destinationId,
                    destinationName,
                    destinationStatus,
                    lowRatedReviews
            );
        }
    }

    // Standard Getters and Setters
    public Long getDestinationId() { return destinationId; }
    public String getDestinationName() { return destinationName; }
    public DestinationStatus getDestinationStatus() { return destinationStatus; }
    public List<DestinationReview> getLowRatedReviews() { return lowRatedReviews; }
    public Integer getLowRatedCount() { return lowRatedCount; }
}