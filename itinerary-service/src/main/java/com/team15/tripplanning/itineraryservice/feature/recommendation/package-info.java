/**
 * M3 — Slice S3 (itinerary-service): Neo4j-based recommendation feature.
 *
 * <p><b>OWNED BY: S3</b> — pull request {@code feature/m3-s3-feign-amqp-itinerary}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code RecommendationController} — exposes:
 *       {@code GET /api/itineraries/{itineraryId}/recommendations}
 *       → {@code List<DestinationSummaryDTO>} (max 5 entries)
 *   <li>{@code RecommendationService} — runs a Cypher collaborative-filter query
 *       against Neo4j: "find destinations visited by users who also visited
 *       the destination in this itinerary but NOT yet visited by this user."
 *       Enriches raw destination IDs with {@code DestinationServiceClient.batchGetDestinations}.
 *       Cached in Redis, key {@code rec:itinerary:{id}}, TTL 10 minutes.
 * </ul>
 */
package com.team15.tripplanning.itineraryservice.feature.recommendation;
