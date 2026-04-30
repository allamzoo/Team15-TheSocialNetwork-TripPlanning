package com.team15.tripplanning.itineraryservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.team15.tripplanning.itineraryservice.controller.ItineraryController;
import com.team15.tripplanning.itineraryservice.model.Itinerary;
import com.team15.tripplanning.itineraryservice.model.ItineraryDay;
import com.team15.tripplanning.itineraryservice.service.ItineraryService;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ItineraryEntityNoBuilderContractTest {

    @Test
    void s3F8FlowUsesEntitiesAndNoBuilderOnEntityModels() throws Exception {
        Method addDaysControllerMethod = ItineraryController.class.getMethod("addDays", Long.class, java.util.List.class);
        assertEquals(ResponseEntity.class, addDaysControllerMethod.getReturnType());

        Method addDaysServiceMethod = ItineraryService.class.getMethod("addDays", Long.class, java.util.List.class);
        assertEquals(Itinerary.class, addDaysServiceMethod.getReturnType());

        assertFalse(hasBuilderMethod(Itinerary.class));
        assertFalse(hasBuilderMethod(ItineraryDay.class));
    }

    private static boolean hasBuilderMethod(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .anyMatch(method -> "builder".equals(method.getName()));
    }
}

