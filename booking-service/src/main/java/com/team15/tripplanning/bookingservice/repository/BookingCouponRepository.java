package com.team15.tripplanning.bookingservice.repository;

import com.team15.tripplanning.bookingservice.model.BookingCoupon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookingCouponRepository extends JpaRepository<BookingCoupon, Long> {
    List<BookingCoupon> findByBooking_Id(Long bookingId);

    List<BookingCoupon> findByCoupon_Id(Long couponId);

    @Query(value = """
            SELECT bc.id, b.id AS booking_id, c.code, bc.discount_applied, bc.applied_at
            FROM booking_coupons bc
            JOIN bookings b ON b.id = bc.booking_id
            JOIN coupons c ON c.id = bc.coupon_id
            WHERE b.user_id = :userId
            """, nativeQuery = true)
    List<Object[]> findCouponApplicationsByUser(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM booking_coupons WHERE booking_id = :bookingId", nativeQuery = true)
    int deleteAllByBookingId(@Param("bookingId") Long bookingId);
}
