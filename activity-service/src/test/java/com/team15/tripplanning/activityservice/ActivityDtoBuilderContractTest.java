package com.team15.tripplanning.activityservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team15.tripplanning.activityservice.dto.ActivitySummaryDTO;
import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActivityDtoBuilderContractTest {

    @Test
    void m1ActivityDtosExposeBuilderContracts() throws Exception {
        assertBuilderContract(NearbyActivityDTO.class, Map.of(
                "activityId", 1L,
                "name", "Museum",
                "category", "CULTURE",
                "latitude", 30.0d,
                "longitude", 31.0d,
                "distanceKm", 2.5d
        ));

        LocalDateTime now = LocalDateTime.of(2026, 4, 30, 10, 0);
        assertBuilderContract(ActivitySummaryDTO.class, Map.of(
                "itineraryId", 2L,
                "totalActivities", 4,
                "averageCost", 60.0d,
                "maxCost", 120.0d,
                "firstScheduledTime", now,
                "lastScheduledTime", now.plusHours(6)
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

