package com.team15.tripplanning.destinationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team15.tripplanning.destinationservice.dto.DestinationRevenueDTO;
import com.team15.tripplanning.destinationservice.dto.DestinationReviewAlertDTO;
import com.team15.tripplanning.destinationservice.dto.TopDestinationDTO;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DestinationDtoBuilderContractTest {

    @Test
    void destinationDashboardDtoFamilyHasAccessibleBuildersWithFluentSetters() throws Exception {
        assertBuilderContract(DestinationRevenueDTO.class, Map.of(
                "destinationId", 101L,
                "name", "Paris",
                "totalBookings", 12L,
                "totalRevenue", 3456.78d,
                "averageBookingAmount", 288.06d
        ));

        assertBuilderContract(TopDestinationDTO.class, Map.of(
                "destinationId", 202L,
                "name", "Tokyo",
                "rating", 4.9d,
                "totalBookings", 77L
        ));

        assertBuilderContract(DestinationReviewAlertDTO.class, Map.of(
                "destinationId", 303L,
                "destinationName", "Rome",
                "destinationStatus", DestinationStatus.ACTIVE,
                "lowRatedReviews", new ArrayList<>()
        ));
    }

    private static void assertBuilderContract(Class<?> dtoClass, Map<String, Object> fieldValues) throws Exception {
        Method builderMethod = dtoClass.getDeclaredMethod("builder");
        assertTrue(Modifier.isStatic(builderMethod.getModifiers()), dtoClass.getSimpleName() + ".builder() must be static");
        assertTrue(Modifier.isPublic(builderMethod.getModifiers()), dtoClass.getSimpleName() + ".builder() must be public");

        Object builder = builderMethod.invoke(null);
        Class<?> builderClass = builder.getClass();

        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            Method setter = builderClass.getMethod(entry.getKey(), dtoClass.getDeclaredField(entry.getKey()).getType());
            assertEquals(builderClass, setter.getReturnType(), dtoClass.getSimpleName() + "." + entry.getKey() + "() must return Builder");
            setter.invoke(builder, entry.getValue());
        }

        Method build = builderClass.getMethod("build");
        Object built = build.invoke(builder);
        assertEquals(dtoClass, built.getClass(), dtoClass.getSimpleName() + ".build() must return the DTO type");
    }
}


