package com.team15.tripplanning.destinationservice.messaging.consumer;

import com.team15.tripplanning.contracts.events.ItineraryPlacedEvent;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.destinationservice.cache.DestinationCacheInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ItineraryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEventConsumer.class);
    private final DestinationCacheInvalidator cacheInvalidator;

    public ItineraryEventConsumer(DestinationCacheInvalidator cacheInvalidator) {
        this.cacheInvalidator = cacheInvalidator;
    }

    @RabbitListener(queues = "destination.itinerary.saga-listener")
    public void onItineraryPlaced(ItineraryPlacedEvent event) {
        log.info("Consumed itinerary.placed for destination={}", event.destinationId());
        cacheInvalidator.invalidateRevenueCache(event.destinationId());
        cacheInvalidator.invalidateDashboardCache(event.destinationId());
    }

    @RabbitListener(queues = "destination.itinerary.saga-listener")
    public void onItineraryCompleted(ItineraryCompletedEvent event) {
        log.info("Consumed itinerary.completed for destination={}", event.destinationId());
        cacheInvalidator.invalidateRevenueCache(event.destinationId());
        cacheInvalidator.invalidateDashboardCache(event.destinationId());
    }

    @RabbitListener(queues = "destination.itinerary.saga-listener")
    public void onItineraryCancelled(ItineraryCancelledEvent event) {
        log.info("Consumed itinerary.cancelled for destination={}", event.destinationId());
        cacheInvalidator.invalidateRevenueCache(event.destinationId());
        cacheInvalidator.invalidateDashboardCache(event.destinationId());
    }
}