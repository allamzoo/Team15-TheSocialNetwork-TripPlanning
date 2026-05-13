/**
 * M3 — Slice S3 (itinerary-service): Neo4j graph-write feature.
 *
 * <p><b>OWNED BY: S3</b> — pull request {@code feature/m3-s3-feign-amqp-itinerary}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code ItineraryGraphService} — writes the tripartite graph on every
 *       {@code ItineraryPlacedEvent}:
 *       {@code (User)-[:PLANNED]->(Itinerary)-[:VISITS]->(Destination)}.
 *       Uses existing Neo4j node models in {@code model.neo4j}.
 *   <li>{@code GraphWriteListener} — {@code @TransactionalEventListener(phase=AFTER_COMMIT)}
 *       that triggers {@code ItineraryGraphService.record()} after the AMQP event
 *       is published, keeping graph writes async and non-blocking for the API caller.
 * </ul>
 *
 * <p>Neo4j node models ({@code UserNode}, {@code DestinationNode}, {@code ItineraryNode})
 * and their repositories live in the existing {@code model.neo4j} and
 * {@code repository} packages — do not duplicate them here.
 */
package com.team15.tripplanning.itineraryservice.feature.graph;
