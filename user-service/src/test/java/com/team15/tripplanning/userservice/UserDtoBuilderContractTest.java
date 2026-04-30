package com.team15.tripplanning.userservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team15.tripplanning.userservice.dto.TopTravelerDTO;
import com.team15.tripplanning.userservice.dto.UserProfileDTO;
import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserDtoBuilderContractTest {

    @Test
    void m1UserDtosExposeBuilderContracts() throws Exception {
        assertBuilderContract(UserTripSummaryDTO.class, Map.of(
                "userId", 1L,
                "name", "Alice",
                "totalTrips", 12L,
                "completedTrips", 9L,
                "cancelledTrips", 3L,
                "totalSpent", 2440.4d,
                "averageBudget", 203.3d
        ));

        assertBuilderContract(TopTravelerDTO.class, Map.of(
                "userId", 2L,
                "name", "Bob",
                "totalSpent", 1800.0d,
                "tripCount", 7L
        ));

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("travelStyle", "ADVENTURE");

        assertBuilderContract(UserProfileDTO.class, Map.of(
                "userId", 3L,
                "name", "Carol",
                "email", "carol@example.com",
                "phone", "+1000000",
                "preferences", preferences,
                "savedDestinations", new ArrayList<>(),
                "totalSavedDestinations", 0L
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

