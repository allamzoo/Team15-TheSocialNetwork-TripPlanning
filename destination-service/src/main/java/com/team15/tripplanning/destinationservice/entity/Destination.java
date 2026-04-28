package com.team15.tripplanning.destinationservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import javax.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@jakarta.persistence.Entity
@Entity
@Table(name = "destinations")
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DestinationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DestinationStatus status = DestinationStatus.ACTIVE;

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(nullable = false)
    private Integer totalRatings = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> details = new HashMap<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DestinationReview> destinationReviews = new ArrayList<>();

    public Destination() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DestinationStatus.ACTIVE;
        }
        if (rating == null) {
            rating = 0.0;
        }
        if (totalRatings == null) {
            totalRatings = 0;
        }
        if (details == null) {
            details = new HashMap<>();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DestinationCategory getCategory() {
        return category;
    }

    public void setCategory(DestinationCategory category) {
        this.category = category;
    }

    public DestinationStatus getStatus() {
        return status;
    }

    public void setStatus(DestinationStatus status) {
        this.status = status;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? details : new HashMap<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @JsonProperty("reviews")
    public List<DestinationReview> getDestinationReviews() {
        return destinationReviews;
    }

    public void setDestinationReviews(List<DestinationReview> reviews) {
        this.destinationReviews = reviews;
        if (reviews != null) {
            // This line is the "Magic Link"
            // It loops through each review and sets the 'destination' field to 'this'
            reviews.forEach(review -> review.setDestination(this));
        }
    }
}
