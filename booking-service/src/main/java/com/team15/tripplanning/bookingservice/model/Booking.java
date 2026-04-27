package com.team15.tripplanning.bookingservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "bookings")
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
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "itinerary_id")
    private Long itineraryId;

    // Backward-compatibility for tests/scripts that insert itin_id directly.
    @Column(name = "itin_id")
    private Long legacyItinId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb default '{}'::jsonb")
    private Map<String, Object> bookingDetails = new HashMap<>();

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

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
        syncItineraryColumns();
    }

    @PreUpdate
    public void preUpdate() {
        syncItineraryColumns();
    }

    private void syncItineraryColumns() {
        if (itineraryId == null && legacyItinId != null) {
            itineraryId = legacyItinId;
        }
        if (legacyItinId == null && itineraryId != null) {
            legacyItinId = itineraryId;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getItineraryId() {
        return itineraryId != null ? itineraryId : legacyItinId;
    }

    public void setItineraryId(Long itineraryId) {
        this.itineraryId = itineraryId;
        this.legacyItinId = itineraryId;
    }
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
    public List<BookingCoupon> getBookingCoupons() { return bookingCoupons; }
    public void setBookingCoupons(List<BookingCoupon> bookingCoupons) {
        this.bookingCoupons = bookingCoupons != null ? bookingCoupons : new ArrayList<>();
    }
}
