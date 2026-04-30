# S2-F11 Quick Reference - API Examples

## Overview
This document provides quick reference examples for using the S2-F11 Destination Search Indexing feature.

## Endpoint Summary

| Method | Endpoint | Auth | Response |
|--------|----------|------|----------|
| POST | /api/destinations/{id}/index | Required | 200, 401, 404 |
| POST | /api/destinations | None | 201 (auto-indexes) |
| PUT | /api/destinations/{id} | None | 200 (auto-indexes) |
| DELETE | /api/destinations/{id} | None | 204 (removes from ES) |

---

## Example 1: Explicit Indexing (Success)

### Request
```bash
curl -X POST http://localhost:8080/api/destinations/1/index \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### Response
```
HTTP/1.1 200 OK
```

### MongoDB Event Created
```json
{
  "_id": "550e8400-e29b-41d4-a716-446655440000",
  "destinationId": 1,
  "action": "INDEXED",
  "timestamp": "2026-04-30T10:30:00",
  "details": {
    "action": "INDEXED",
    "destinationId": 1,
    "indexedFields": [
      "id", "name", "country", "category", "description",
      "highlights", "rating", "totalRatings", "status"
    ],
    "source": "explicit"
  }
}
```

---

## Example 2: Create Destination (Auto-Index)

### Request
```bash
curl -X POST http://localhost:8080/api/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Santorini",
    "country": "Greece",
    "description": "idyllic Greek island with caldera views",
    "category": "BEACH",
    "status": "ACTIVE",
    "rating": 4.8,
    "totalRatings": 250,
    "details": {
      "topAttractions": ["Oia sunset", "Red Beach", "Caldera views"]
    }
  }'
```

### Response
```json
{
  "id": 1,
  "name": "Santorini",
  "country": "Greece",
  "description": "idyllic Greek island with caldera views",
  "category": "BEACH",
  "status": "ACTIVE",
  "rating": 4.8,
  "totalRatings": 250,
  "details": {
    "topAttractions": ["Oia sunset", "Red Beach", "Caldera views"]
  }
}
```

### Elasticsearch Document Created
```json
{
  "_index": "destinations",
  "_id": "1",
  "_source": {
    "id": 1,
    "name": "Santorini",
    "country": "Greece",
    "category": "BEACH",
    "description": "idyllic Greek island with caldera views",
    "highlights": "Oia sunset Red Beach Caldera views",
    "rating": 4.8,
    "totalRatings": 250,
    "status": "ACTIVE"
  }
}
```

### MongoDB Event Created
```json
{
  "destinationId": 1,
  "action": "INDEXED",
  "source": "auto_crud_create",
  "indexedFields": [...]
}
```

---

## Example 3: Update Destination (Auto-Index)

### Request
```bash
curl -X PUT http://localhost:8080/api/destinations/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Santorini - Paradise Island",
    "country": "Greece",
    "description": "Updated description: idyllic Greek island with stunning caldera views",
    "category": "BEACH",
    "status": "ACTIVE",
    "rating": 4.9,
    "totalRatings": 300,
    "details": {
      "topAttractions": ["Oia sunset", "Red Beach", "Fira town", "Hot springs"]
    }
  }'
```

### Response
```json
{
  "id": 1,
  "name": "Santorini - Paradise Island",
  "country": "Greece",
  "description": "Updated description: idyllic Greek island with stunning caldera views",
  ...
}
```

### Elasticsearch Document Updated
```json
{
  "_index": "destinations",
  "_id": "1",
  "_source": {
    "id": 1,
    "name": "Santorini - Paradise Island",
    "highlights": "Oia sunset Red Beach Fira town Hot springs",
    "description": "Updated description: idyllic Greek island with stunning caldera views",
    ...
  }
}
```

### MongoDB Event Created
```json
{
  "destinationId": 1,
  "action": "INDEXED",
  "source": "auto_crud_update",
  "indexedFields": [...]
}
```

---

## Example 4: Delete Destination (Auto-Remove)

### Request
```bash
curl -X DELETE http://localhost:8080/api/destinations/1
```

### Response
```
HTTP/1.1 204 No Content
```

### Elasticsearch Document Removed
- Document with ID 1 is deleted from "destinations" index

### MongoDB Event Created
```json
{
  "_id": "550e8400-e29b-41d4-a716-446655440001",
  "destinationId": 1,
  "action": "DESTINATION_DELETED",
  "timestamp": "2026-04-30T10:35:00",
  "details": {
    "action": "DESTINATION_DELETED",
    "destinationId": 1,
    "source": "auto_crud_delete"
  }
}
```

---

## Example 5: Missing Token (Unauthorized)

### Request
```bash
curl -X POST http://localhost:8080/api/destinations/1/index \
  -H "Content-Type: application/json"
```

### Response
```
HTTP/1.1 401 Unauthorized

{
  "status": 401,
  "message": "Unauthorized"
}
```

---

## Example 6: Non-Existent Destination (Not Found)

### Request
```bash
curl -X POST http://localhost:8080/api/destinations/999/index \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### Response
```
HTTP/1.1 404 Not Found

{
  "timestamp": "2026-04-30T10:40:00",
  "status": 404,
  "error": "Not Found",
  "message": "Destination not found: 999",
  "path": "/api/destinations/999/index"
}
```

---

## Example 7: Destination Without topAttractions

### Request
```bash
curl -X POST http://localhost:8080/api/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Unknown Beach",
    "country": "Unknown Country",
    "description": "A mysterious beach",
    "category": "BEACH",
    "status": "ACTIVE",
    "rating": 3.5,
    "totalRatings": 50,
    "details": {}
  }'
```

### Elasticsearch Document Created
```json
{
  "_index": "destinations",
  "_id": "2",
  "_source": {
    "id": 2,
    "name": "Unknown Beach",
    "country": "Unknown Country",
    "category": "BEACH",
    "description": "A mysterious beach",
    "highlights": "",
    "rating": 3.5,
    "totalRatings": 50,
    "status": "ACTIVE"
  }
}
```

**Note**: The `highlights` field is empty string because `topAttractions` was not provided.

---

## Example 8: Verify Elasticsearch Document

### Request
```bash
curl http://localhost:9200/destinations/_doc/1
```

### Response
```json
{
  "_index": "destinations",
  "_type": "_doc",
  "_id": "1",
  "_version": 1,
  "found": true,
  "_source": {
    "id": 1,
    "name": "Santorini",
    "country": "Greece",
    "category": "BEACH",
    "description": "idyllic Greek island with caldera views",
    "highlights": "Oia sunset Red Beach Caldera views",
    "rating": 4.8,
    "totalRatings": 250,
    "status": "ACTIVE"
  }
}
```

---

## Example 9: Verify MongoDB Event

### MongoDB Query
```javascript
// Connect to MongoDB
mongo mongodb://localhost:27017/tripplanningdb

// Query destination_events collection
db.destination_events.findOne({ destinationId: 1, action: "INDEXED" })
```

### Response
```json
{
  "_id": ObjectId("550e8400e29b41d4a716446655440000"),
  "destinationId": 1,
  "action": "INDEXED",
  "timestamp": ISODate("2026-04-30T10:30:00.000Z"),
  "details": {
    "action": "INDEXED",
    "destinationId": 1,
    "indexedFields": [
      "id", "name", "country", "category", "description",
      "highlights", "rating", "totalRatings", "status"
    ],
    "source": "explicit"
  }
}
```

---

## Example 10: Search Elasticsearch for Indexed Destination

### Request (Search by keyword)
```bash
curl -X GET http://localhost:9200/destinations/_search \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "multi_match": {
        "query": "caldera",
        "fields": ["description", "highlights"]
      }
    }
  }'
```

### Response
```json
{
  "took": 5,
  "timed_out": false,
  "hits": {
    "total": { "value": 1, "relation": "eq" },
    "hits": [
      {
        "_index": "destinations",
        "_id": "1",
        "_score": 2.5,
        "_source": {
          "id": 1,
          "name": "Santorini",
          "country": "Greece",
          "category": "BEACH",
          "description": "idyllic Greek island with caldera views",
          "highlights": "Oia sunset Red Beach Caldera views",
          "rating": 4.8,
          "totalRatings": 250,
          "status": "ACTIVE"
        }
      }
    ]
  }
}
```

---

## Common Response Codes

| Code | Scenario | Example |
|------|----------|---------|
| 200 | Successful indexing | POST /api/destinations/1/index with valid token |
| 201 | Destination created & auto-indexed | POST /api/destinations |
| 204 | Destination deleted & removed from ES | DELETE /api/destinations/1 |
| 400 | Invalid request body | Invalid JSON in POST |
| 401 | Missing/invalid JWT token | POST /index without token |
| 404 | Destination not found | POST /index with non-existent ID |
| 500 | Internal server error | Elasticsearch/DB connection failure |

---

## Troubleshooting

### Issue: 401 Unauthorized
**Cause**: Missing or invalid JWT token
**Solution**: 
```bash
# Generate/get a valid token and include in header
curl -X POST http://localhost:8080/api/destinations/1/index \
  -H "Authorization: Bearer {VALID_TOKEN}"
```

### Issue: 404 Not Found
**Cause**: Destination doesn't exist
**Solution**: 
- Create destination first via POST /api/destinations
- Use correct destination ID

### Issue: Document not appearing in Elasticsearch
**Cause**: Indexing failed or Elasticsearch not running
**Solution**:
- Verify Elasticsearch is running: `curl http://localhost:9200`
- Check application logs for errors
- Verify network connectivity

### Issue: Events not appearing in MongoDB
**Cause**: MongoDB not running or event logging failed
**Solution**:
- Verify MongoDB is running
- Check MongoDB is accessible
- Review application logs

### Issue: highlights field empty despite topAttractions
**Cause**: topAttractions not in details map
**Solution**:
```json
{
  "details": {
    "topAttractions": ["attraction1", "attraction2"]
  }
}
```

---

## Testing Sequence

1. **Create destination with topAttractions**
   ```bash
   POST /api/destinations
   ```

2. **Verify in Elasticsearch**
   ```bash
   GET http://localhost:9200/destinations/_doc/{id}
   ```

3. **Verify in MongoDB**
   ```javascript
   db.destination_events.findOne({ destinationId: {id}, action: "INDEXED" })
   ```

4. **Explicit re-index**
   ```bash
   POST /api/destinations/{id}/index
   ```

5. **Verify second INDEXED event**
   ```javascript
   db.destination_events.find({ destinationId: {id}, action: "INDEXED" }).sort({ timestamp: -1 }).limit(2)
   ```

6. **Update destination**
   ```bash
   PUT /api/destinations/{id}
   ```

7. **Search Elasticsearch for updated content**
   ```bash
   GET http://localhost:9200/destinations/_search
   ```

8. **Delete destination**
   ```bash
   DELETE /api/destinations/{id}
   ```

9. **Verify removal from Elasticsearch**
   ```bash
   GET http://localhost:9200/destinations/_doc/{id}  # Should return 404
   ```

10. **Verify DESTINATION_DELETED event**
    ```javascript
    db.destination_events.findOne({ destinationId: {id}, action: "DESTINATION_DELETED" })
    ```

---

## Performance Notes

- **Auto-indexing**: Happens synchronously with CRUD operations
- **Elasticsearch latency**: Typically < 100ms per operation
- **MongoDB event logging**: Happens asynchronously via observer pattern
- **Search performance**: Depends on query complexity and data volume

---

## Security Notes

- JWT token required for explicit indexing endpoint only
- Auto-indexing happens without additional authentication (uses service-layer)
- All Elasticsearch/MongoDB operations are scoped to current service instance
- No cross-service authentication checks needed

---

## Related Documentation

- [S2-F11 Full Implementation Guide](./S2-F11_IMPLEMENTATION.md)
- [S2-F11 Deployment Checklist](./S2-F11_DEPLOYMENT_CHECKLIST.md)

