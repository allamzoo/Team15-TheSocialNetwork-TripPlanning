# S2-F11 Destination Search Indexing Implementation

## Overview
This document describes the implementation of the S2-F11 feature for indexing destinations in Elasticsearch, with support for both explicit and automatic indexing on CRUD operations.

## Architecture

### Components Created/Modified

#### 1. **DestinationSearchDocument** (New)
- **File**: `destination-service/src/main/java/com/team15/tripplanning/destinationservice/model/DestinationSearchDocument.java`
- **Purpose**: Elasticsearch document model for destination search
- **Fields**:
  - `id`: Destination ID (Long)
  - `name`: Destination name (Text)
  - `country`: Destination country (Text)
  - `category`: Destination category (Text)
  - `description`: Destination description (Text)
  - `highlights`: Extracted from topAttractions (Text)
  - `rating`: Destination rating (Double)
  - `totalRatings`: Total number of ratings (Integer)
  - `status`: Destination status (Keyword)

#### 2. **DestinationSearchRepository** (New)
- **File**: `destination-service/src/main/java/com/team15/tripplanning/destinationservice/repository/DestinationSearchRepository.java`
- **Purpose**: Spring Data Elasticsearch repository for CRUD operations on search documents
- **Extends**: `ElasticsearchRepository<DestinationSearchDocument, Long>`

#### 3. **DestinationSearchService** (New)
- **File**: `destination-service/src/main/java/com/team15/tripplanning/destinationservice/service/DestinationSearchService.java`
- **Purpose**: Service layer for Elasticsearch operations
- **Key Methods**:
  - `indexDestination(Destination, String source)`: Creates/updates destination in Elasticsearch
    - Returns map containing `indexedFields` list and `highlights` string
    - Supports sources: "explicit", "auto_crud_create", "auto_crud_update"
  - `removeDestination(Long id)`: Removes destination from Elasticsearch
  - `extractHighlights(Map<String, Object>)`: Extracts highlights from details.topAttractions

#### 4. **DestinationService** (Modified)
- **Changes**:
  - Added `DestinationSearchService` dependency
  - Modified `create()` method to auto-index with source="auto_crud_create"
  - Modified `update()` method to auto-index with source="auto_crud_update"
  - Modified `delete()` method to remove from Elasticsearch
  - Added `indexDestinationExplicit(Long id)` method for explicit indexing

#### 5. **DestinationController** (Modified)
- **Changes**:
  - Added `POST /api/destinations/{id}/index` endpoint
  - Requires authentication via `@PreAuthorize("isAuthenticated()")`
  - Returns HTTP 200 on success
  - Returns HTTP 404 if destination not found
  - Returns HTTP 401 if not authenticated

## API Endpoint

### Explicit Index Endpoint

**Endpoint**: `POST /api/destinations/{id}/index`

**Authentication**: Required (USER role)

**Path Parameters**:
- `id` (Long): The ID of the destination to index

**Request Headers**:
- `Authorization: Bearer {jwt_token}` (Required)
- `Content-Type: application/json`

**Response**:
- **200 OK**: Successfully indexed
- **404 Not Found**: Destination not found
- **401 Unauthorized**: Missing or invalid JWT token

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/destinations/1/index \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json"
```

## Behavior

### 1. Explicit Indexing (Manual)
When `POST /api/destinations/{id}/index` is called:
1. JWT token is validated → throws 401 if missing/invalid
2. Destination is found by ID in PostgreSQL → throws 404 if not found
3. Description and topAttractions are read from the destination
4. Highlights field is generated:
   - If topAttractions is missing/empty → empty string
   - Otherwise → space-separated string of attractions
5. Elasticsearch document is created/updated with all fields
6. INDEXED event is logged to MongoDB with:
   - `destinationId`: The destination ID
   - `indexedFields`: ["id", "name", "country", "category", "description", "highlights", "rating", "totalRatings", "status"]
   - `source`: "explicit"
7. Returns HTTP 200

### 2. Auto-Indexing on CRUD (Automatic)
The destination is automatically re-indexed on any CRUD operation:

#### On CREATE:
- Elasticsearch document is created with source="auto_crud_create"
- INDEXED event logged with source="auto_crud_create"

#### On UPDATE:
- Elasticsearch document is updated with new values
- INDEXED event logged with source="auto_crud_update"

#### On DELETE:
- Elasticsearch document is removed
- DESTINATION_DELETED event is logged
- Source in event: "auto_crud_delete"

## MongoDB Event Structure

### INDEXED Event (for Create, Update, and explicit Index)
```json
{
  "_id": "auto-generated-uuid",
  "destinationId": 1,
  "action": "INDEXED",
  "timestamp": "2026-04-30T10:30:00",
  "details": {
    "action": "INDEXED",
    "destinationId": 1,
    "indexedFields": ["id", "name", "country", "category", "description", "highlights", "rating", "totalRatings", "status"],
    "source": "explicit|auto_crud_create|auto_crud_update"
  }
}
```

### DESTINATION_DELETED Event (for Delete)
```json
{
  "_id": "auto-generated-uuid",
  "destinationId": 1,
  "action": "DESTINATION_DELETED",
  "timestamp": "2026-04-30T10:30:00",
  "details": {
    "action": "DESTINATION_DELETED",
    "destinationId": 1,
    "source": "auto_crud_delete"
  }
}
```

## Test Scenarios

### Test Case 1: Explicit Indexing - Success
**Scenario**: Create a destination and then explicitly index it
- Create destination "Santorini" with description="idyllic Greek island with caldera views" and details.topAttractions=["Oia sunset", "Red Beach"]
- Call POST /api/destinations/{id}/index with valid token
- Expected: Returns 200
- Verify: 
  - Search with query="caldera" returns the destination
  - INDEXED event exists with source="explicit" and indexedFields listed

### Test Case 2: Missing topAttractions
**Scenario**: Index a destination without topAttractions
- Create destination with details JSONB without topAttractions key
- Call POST /api/destinations/{id}/index
- Expected: Indexing succeeds with empty highlights field

### Test Case 3: Non-existent Destination
**Scenario**: Try to index a non-existent destination
- Call POST /api/destinations/999/index
- Expected: Returns 404

### Test Case 4: Auto-Indexing on Update
**Scenario**: Update a destination's name without calling /index endpoint
- Create destination via CRUD
- Update destination name via PUT /api/destinations/{id}
- Search with new name
- Expected: Updated destination found in search
- Verify: Second INDEXED event appeared with source="auto_crud_update"

### Test Case 5: Auto-Removal on Delete
**Scenario**: Delete a destination
- Create destination
- Delete via DELETE /api/destinations/{id}
- Search for deleted destination
- Expected: Empty result list
- Verify: DESTINATION_DELETED event logged

### Test Case 6: Missing Token
**Scenario**: Try to index without authentication
- Call POST /api/destinations/{id}/index without token
- Expected: Returns 401 Unauthorized

## Implementation Details

### Highlights Extraction Logic
```java
private String extractHighlights(Map<String, Object> details) {
    if (details == null) return "";
    
    Object topAttractions = details.get("topAttractions");
    if (topAttractions == null || !(topAttractions instanceof List<?>)) return "";
    
    List<?> list = (List<?>) topAttractions;
    if (list.isEmpty()) return "";
    
    // Join list items with space separator
    return String.join(" ", list.stream().map(Object::toString).toList());
}
```

### Event Logging Flow
1. Service layer calls `searchService.indexDestination()` or `searchService.removeDestination()`
2. Service layer builds event payload with indexed fields and source
3. Observer chain calls `notifyObservers("INDEXED", payload)` or `notifyObservers("DESTINATION_DELETED", payload)`
4. MongoEventLogger receives event and saves to MongoDB destination_events collection

## Error Handling

### 401 Unauthorized
- JWT token is missing from Authorization header
- JWT token is invalid or expired
- Handled by Spring Security filter chain

### 404 Not Found
- Destination ID does not exist in PostgreSQL
- Handled by ResponseStatusException in findById()

### 500 Internal Server Error
- Elasticsearch connection failure or document indexing failure
- Wrapped in RuntimeException with descriptive message
- Logs detailed error message for debugging

## Configuration

The implementation requires:

1. **PostgreSQL**: Destination entity is persisted here
2. **Elasticsearch**: Search documents are indexed here
   - Index name: "destinations"
   - Configuration in application.yml:
     ```yaml
     spring:
       elasticsearch:
         uris: ${SPRING_ELASTICSEARCH_URIS:http://localhost:9200}
     ```

3. **MongoDB**: Events are logged here
   - Collection: "destination_events"
   - Configuration in application.yml:
     ```yaml
     spring:
       data:
         mongodb:
           uri: ${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/tripplanningdb}
     ```

4. **Security**: JWT authentication is enforced
   - Uses Spring Security with @PreAuthorize
   - JWT validation through JwtService

## Dependencies Added

The implementation uses existing dependencies from pom.xml:
- `spring-boot-starter-data-elasticsearch`: For Elasticsearch integration
- `spring-boot-starter-security`: For authentication/authorization
- `jjwt-*`: For JWT token handling
- `spring-boot-starter-data-mongodb`: For event logging

No new external dependencies are required.

## Future Enhancements

1. Add full-text search capabilities across all indexed fields
2. Implement search filters (by rating, country, category)
3. Add bulk indexing for performance optimization
4. Implement async indexing to avoid blocking requests
5. Add cache invalidation strategy for search results
6. Implement search analytics and tracking

## Testing

Integration tests are provided in:
- `destination-service/src/test/java/com/team15/tripplanning/destinationservice/integration/DestinationSearchIntegrationTest.java`

Tests cover:
- Explicit indexing success and error cases
- Auto-indexing on CRUD operations
- Authorization and authentication
- Highlights extraction with and without topAttractions
- Event logging verification

