package com.team15.tripplanning.bookingservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Table(name = "bookings")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class Booking {
    public enum BookingType {
        ACCOMMODATION,
        TRANSPORT,
        ACTIVITY
    }

    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        CANCELLED,
        PLANNED,
        COMPLETED,
        IN_PROGRESS,
        REFUNDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonAlias({"itinerary_id"})
    @Column(name = "itin_id", nullable = false)
    private Long itineraryId;


    @JsonAlias({"user_id"})
    @Column(nullable = false)
    private Long userId;

    @JsonAlias({"booking_amount"})
    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @JsonAlias({"booking_type"})
    @Column(nullable = false, columnDefinition = "bookingtype default 'ACCOMMODATION'")
    private BookingType type;

    @Enumerated(EnumType.STRING)
    @JsonAlias({"booking_status"})
    @Column(nullable = false, columnDefinition = "VARCHAR(50) default 'PENDING'")
    private BookingStatus status = BookingStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @JsonAlias({"booking_details"})
    @Column(columnDefinition = "jsonb default '{}'::jsonb")
    private Map<String, Object> bookingDetails = new HashMap<>();

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingCoupon> bookingCoupons = new ArrayList<>();

    public Booking() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingStatus.PENDING;
        }
        if (bookingDetails == null) {
            bookingDetails = new HashMap<>();
        }
    }

    @PreUpdate
    public void preUpdate() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public BookingType getType() { return type; }
    public void setType(BookingType type) { this.type = type; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public Map<String, Object> getBookingDetails() { return bookingDetails; }
    public void setBookingDetails(Map<String, Object> bookingDetails) {
        this.bookingDetails = bookingDetails != null ? bookingDetails : new HashMap<>();
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public List<BookingCoupon> getBookingCoupons() { return bookingCoupons; }
    public void setBookingCoupons(List<BookingCoupon> bookingCoupons) {
        this.bookingCoupons = bookingCoupons != null ? bookingCoupons : new ArrayList<>();
    }
}


