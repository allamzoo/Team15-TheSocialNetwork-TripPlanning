package com.team15.tripplanning.activityservice.feign;

import com.team15.tripplanning.activityservice.feign.dto.ItineraryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "itinerary-service", url = "${feign.itinerary-service.url}")
public interface ItineraryServiceClient {

    @GetMapping("/api/itineraries/{itineraryId}")
    ItineraryDTO getItinerary(@PathVariable Long itineraryId);
}