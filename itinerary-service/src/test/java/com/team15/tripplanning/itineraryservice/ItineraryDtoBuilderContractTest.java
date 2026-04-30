package com.team15.tripplanning.itineraryservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team15.tripplanning.itineraryservice.dto.ItineraryAnalyticsDTO;
import com.team15.tripplanning.itineraryservice.dto.ItineraryDetailsDTO;
import com.team15.tripplanning.itineraryservice.dto.TripCostEstimateDTO;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItineraryDtoBuilderContractTest {

    @Test
    void m1ItineraryDtosExposeBuilderContracts() throws Exception {
        assertBuilderContract(TripCostEstimateDTO.class, Map.of(
                "estimatedAccommodation", 500.0d,
                "estimatedTransport", 200.0d,
                "estimatedActivities", 300.0d,
                "estimatedTotal", 1000.0d,
                "seasonMultiplier", 1.2d
        ));

        assertBuilderContract(ItineraryAnalyticsDTO.class, Map.of(
                "totalItineraries", 40L,
                "completedItineraries", 30L,
                "cancelledItineraries", 5L,
                "totalBudget", 8000.0d,
                "averageBudget", 200.0d,
                "completionRate", 75.0d
        ));

        assertBuilderContract(ItineraryDetailsDTO.class, Map.of(
                "itineraryId", 77L,
                "userId", 88L,
                "destinationId", 99L,
                "title", "Spring Trip",
                "status", "PLANNED",
                "estimatedBudget", 500.0d,
                "metadata", Map.of("season", "spring"),
                "days", new ArrayList<>(),
                "totalDays", 0,
                "completedDays", 0
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

