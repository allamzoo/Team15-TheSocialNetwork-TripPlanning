package com.team15.tripplanning.bookingservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team15.tripplanning.bookingservice.dto.BookingDetailsDTO;
import com.team15.tripplanning.bookingservice.dto.CouponUsageDTO;
import com.team15.tripplanning.bookingservice.dto.RevenueReportDTO;
import com.team15.tripplanning.bookingservice.dto.UserBookingSummaryDTO;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BookingDtoBuilderContractTest {

    @Test
    void m1BookingDtosExposeBuilderContracts() throws Exception {
        assertBuilderContract(UserBookingSummaryDTO.class, Map.of(
                "userId", 10L,
                "totalBookings", 3,
                "totalAmount", 550.0d,
                "typeBreakdown", Map.of("HOTEL", 300.0d)
        ));

        assertBuilderContract(RevenueReportDTO.class, Map.of(
                "totalRevenue", 5000.0d,
                "totalBookings", 50L,
                "averageBookingAmount", 100.0d,
                "cancelledAmount", 200.0d,
                "cancelledCount", 2L
        ));

        assertBuilderContract(BookingDetailsDTO.class, Map.of(
                "bookingId", 4L,
                "itineraryId", 5L,
                "userId", 6L,
                "originalAmount", 230.0d,
                "type", "HOTEL",
                "status", "CONFIRMED",
                "bookingDetails", new HashMap<>(),
                "appliedCoupons", new ArrayList<>(),
                "totalDiscount", 20.0d,
                "finalAmount", 210.0d
        ));

        assertBuilderContract(CouponUsageDTO.class, Map.of(
                "couponId", 7L,
                "code", "SAVE20",
                "discountType", "PERCENT",
                "discountValue", 20.0d,
                "timesUsed", 3,
                "totalDiscountGiven", 60.0d,
                "active", true,
                "expired", false
        ));
    }

    private static void assertBuilderContract(Class<?> dtoClass, Map<String, Object> fieldValues) throws Exception {
        Method builderMethod = dtoClass.getDeclaredMethod("builder");
        assertTrue(Modifier.isPublic(builderMethod.getModifiers()));
        assertTrue(Modifier.isStatic(builderMethod.getModifiers()));

        Object builder = builderMethod.invoke(null);
        Class<?> builderClass = builder.getClass();

        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            Method setter = builderClass.getMethod(entry.getKey(), dtoClass.getDeclaredField(entry.getKey()).getType());
            assertEquals(builderClass, setter.getReturnType());
            setter.invoke(builder, entry.getValue());
        }

        Method build = builderClass.getMethod("build");
        Object built = build.invoke(builder);
        assertEquals(dtoClass, built.getClass());
    }
}

