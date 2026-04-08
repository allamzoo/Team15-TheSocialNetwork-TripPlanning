package com.team15.tripplanning.bookingservice.repository;

import com.team15.tripplanning.bookingservice.model.Coupon;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    List<Coupon> findByActiveTrue();

    @Query(value = """
            SELECT c.id, c.code, c.discount_type, c.current_uses, COUNT(bc.id) AS used_count
            FROM coupons c
            LEFT JOIN booking_coupons bc ON bc.coupon_id = c.id
            GROUP BY c.id, c.code, c.discount_type, c.current_uses
            """, nativeQuery = true)
    List<Object[]> findCouponUsageSummary();

    @Modifying
    @Transactional
    @Query(value = "UPDATE coupons SET current_uses = current_uses + 1 WHERE id = :id", nativeQuery = true)
    int incrementCurrentUses(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE coupons SET active = FALSE WHERE expiry_date < :cutoff", nativeQuery = true)
    int deactivateExpiredCoupons(@Param("cutoff") LocalDateTime cutoff);
}
