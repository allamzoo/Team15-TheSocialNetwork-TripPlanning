package com.team15.tripplanning.itineraryservice.repository;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

@Repository
public class VisitGraphRepository {

    private final Neo4jClient neo4jClient;

    public VisitGraphRepository(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public boolean isItineraryAlreadyRecorded(Long userId, Long destinationId, Long itineraryId) {
        String query = """
                OPTIONAL MATCH (u:UserNode {userId: $userId})-[r:VISITED]->(d:DestinationNode {destinationId: $destinationId})
                RETURN CASE
                    WHEN r IS NOT NULL AND $itineraryId IN coalesce(r.recorded_itinerary_ids, [])
                    THEN true
                    ELSE false
                END AS recorded
                """;

        return neo4jClient.query(query)
                .bind(userId).to("userId")
                .bind(destinationId).to("destinationId")
                .bind(itineraryId).to("itineraryId")
                .fetchAs(Boolean.class)
                .one()
                .orElse(false);
    }

    public long recordVisit(Long userId,
                            String userName,
                            Long destinationId,
                            String destinationName,
                            String country,
                            String category,
                            Long itineraryId) {

        String query = """
                MERGE (u:UserNode {userId: $userId})
                ON CREATE SET u.name = $userName
                ON MATCH SET u.name = $userName

                MERGE (d:DestinationNode {destinationId: $destinationId})
                ON CREATE SET d.name = $destinationName, d.country = $country, d.category = $category
                ON MATCH SET d.name = $destinationName, d.country = $country, d.category = $category

                MERGE (u)-[r:VISITED]->(d)
                WITH r, coalesce(r.recorded_itinerary_ids, []) AS ids
                SET r.visitCount = CASE
                        WHEN $itineraryId IN ids THEN coalesce(r.visitCount, 0)
                        ELSE coalesce(r.visitCount, 0) + 1
                    END,
                    r.lastVisitDate = CASE
                        WHEN $itineraryId IN ids THEN r.lastVisitDate
                        ELSE localdatetime()
                    END,
                    r.recorded_itinerary_ids = CASE
                        WHEN $itineraryId IN ids THEN ids
                        ELSE ids + $itineraryId
                    END
                RETURN r.visitCount AS visitCount
                """;

        return neo4jClient.query(query)
                .bind(userId).to("userId")
                .bind(userName).to("userName")
                .bind(destinationId).to("destinationId")
                .bind(destinationName).to("destinationName")
                .bind(country).to("country")
                .bind(category).to("category")
                .bind(itineraryId).to("itineraryId")
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
    }

    public long getVisitCount(Long userId, Long destinationId) {
        String query = """
                MATCH (:UserNode {userId: $userId})-[r:VISITED]->(:DestinationNode {destinationId: $destinationId})
                RETURN coalesce(r.visitCount, 0) AS visitCount
                """;

        return neo4jClient.query(query)
                .bind(userId).to("userId")
                .bind(destinationId).to("destinationId")
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
    }
}