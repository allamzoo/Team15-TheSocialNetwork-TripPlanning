# S2-F11 Quick Start Guide for Developers

## 🚀 Get Started in 5 Minutes

### 1. Understand the Feature
**What**: Index destinations in Elasticsearch for faster searching  
**When**: Automatic on create/update/delete, or manual via endpoint  
**Why**: Enable full-text search on destination details

---

## 📍 Where Are the Files?

```
destination-service/
├── src/main/java/com/team15/tripplanning/destinationservice/
│   ├── model/
│   │   └── DestinationSearchDocument.java       ← Elasticsearch model
│   ├── repository/
│   │   └── DestinationSearchRepository.java     ← Data access layer
│   ├── service/
│   │   ├── DestinationService.java              ← MODIFIED (auto-indexing)
│   │   └── DestinationSearchService.java        ← NEW (search logic)
│   └── controller/
│       └── DestinationController.java           ← MODIFIED (new endpoint)
└── src/test/java/.../integration/
    └── DestinationSearchIntegrationTest.java    ← NEW (tests)
```

---

## 🔍 Key Code Locations

### 1. Explicit Indexing Endpoint
**File**: `DestinationController.java` (line 62-66)
```java
@PostMapping("/{id}/index")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Void> indexDestination(@PathVariable Long id) {
    destinationService.indexDestinationExplicit(id);
    return ResponseEntity.ok().build();
}
```

### 2. Auto-Indexing on Create
**File**: `DestinationService.java` (line 71-87)
```java
public Destination create(Destination destination) {
    Destination saved = destinationRepository.save(destination);
    
    // Auto-index in Elasticsearch
    Map<String, Object> indexResult = searchService.indexDestination(saved, "auto_crud_create");
    
    // Log event
    Map<String, Object> payload = new HashMap<>();
    payload.put("destinationId", saved.getId());
    payload.put("indexedFields", indexResult.get("indexedFields"));
    payload.put("source", "auto_crud_create");
    notifyObservers("INDEXED", payload);
    
    // Clear caches
    deleteWildcard("s2-destinations::*");
    return saved;
}
```

### 3. Elasticsearch Document Model
**File**: `DestinationSearchDocument.java`
```java
@Document(indexName = "destinations")
public class DestinationSearchDocument {
    private Long id;
    private String name;
    private String country;
    private String category;
    private String description;
    private String highlights;  // ← Extracted from topAttractions
    private Double rating;
    private Integer totalRatings;
    private String status;
}
```

### 4. Search Service Logic
**File**: `DestinationSearchService.java`
```java
public Map<String, Object> indexDestination(Destination destination, String source) {
    String highlights = extractHighlights(destination.getDetails());
    
    DestinationSearchDocument document = new DestinationSearchDocument(
        destination.getId(),
        destination.getName(),
        // ... other fields ...
        highlights  // ← From topAttractions
    );
    
    searchRepository.save(document);
    return result;
}
```

### 5. Highlights Extraction Logic
**File**: `DestinationSearchService.java` (line 95-120)
```java
private String extractHighlights(Map<String, Object> details) {
    if (details == null) return "";
    
    Object topAttractions = details.get("topAttractions");
    if (!(topAttractions instanceof List<?>)) return "";
    
    List<?> list = (List<?>) topAttractions;
    // Join with spaces: ["Oia", "Beach"] → "Oia Beach"
    return String.join(" ", list.stream().map(Object::toString).toList());
}
```

---

## 🧪 Testing the Implementation

### Test 1: Create & Auto-Index
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

**Expected**: 
- Destination created
- Auto-indexed in Elasticsearch
- INDEXED event logged to MongoDB

### Test 2: Explicit Indexing
```bash
curl -X POST http://localhost:8080/api/destinations/1/index \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected**: 
- Returns 200 OK
- INDEXED event with source="explicit"

### Test 3: Verify in Elasticsearch
```bash
curl http://localhost:9200/destinations/_doc/1
```

**Expected**: Document with all fields including highlights

### Test 4: Verify in MongoDB
```bash
mongo mongodb://localhost:27017/tripplanningdb
db.destination_events.find({ destinationId: 1 }).pretty()
```

**Expected**: INDEXED events with source and indexedFields

### Test 5: Update & Auto-Index
```bash
curl -X PUT http://localhost:8080/api/destinations/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Santorini",
    "description": "Updated description with caldera",
    ...
  }'
```

**Expected**: 
- Updated in ES with new values
- INDEXED event with source="auto_crud_update"

### Test 6: Delete & Auto-Remove
```bash
curl -X DELETE http://localhost:8080/api/destinations/1
```

**Expected**: 
- Removed from ES
- DESTINATION_DELETED event logged

---

## 🐛 Debugging Tips

### No documents in Elasticsearch?
1. Check Elasticsearch is running: `curl http://localhost:9200`
2. Check logs for errors
3. Verify index exists: `curl http://localhost:9200/destinations`

### Events not in MongoDB?
1. Check MongoDB is running and accessible
2. Check application logs for MongoEventLogger errors
3. Verify collection exists: `db.destination_events.find().limit(1)`

### 401 Unauthorized on /index endpoint?
1. Include valid JWT token: `-H "Authorization: Bearer {token}"`
2. Check token is not expired
3. Verify Spring Security is configured

### 404 Not Found?
1. Verify destination exists: `GET /api/destinations/{id}`
2. Use correct destination ID
3. Create destination first before indexing

---

## 🔧 Common Tasks

### Add a New Searchable Field
1. Add property to `DestinationSearchDocument`
2. Add @Field annotation with type
3. Update `DestinationSearchService.indexDestination()`
4. Update `extractHighlights()` if applicable
5. Run tests

### Change Highlights Generation
1. Modify `extractHighlights()` method
2. Update logic for topAttractions
3. Run tests with different inputs
4. Verify in MongoDB events

### Add Custom Search Logic
1. Create new method in `DestinationSearchService`
2. Use `searchRepository` for queries
3. Call from `DestinationController` or elsewhere
4. Add tests for new functionality

---

## 📊 Data Flow Diagram

```
User Request
    ↓
┌─────────────────────────────────────┐
│   DestinationController             │
│   POST /destinations/{id}/index     │
│   (or CRUD operations)              │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│   DestinationService                │
│   - create() [auto-index]           │
│   - update() [auto-index]           │
│   - delete() [remove from ES]       │
│   - indexDestinationExplicit()      │
└─────────────────────────────────────┘
    ↓ (saves entity)          ↓ (indexes document)
PostgreSQL                 DestinationSearchService
  │                           │
  │                     (extract highlights)
  │                           │
  │                    DestinationSearchRepository
  │                           │
  │                      Elasticsearch
  │                      (destinations index)
  │
  └────→ notifyObservers()
         │
         └────→ MongoEventLogger
                │
                MongoDB
                (destination_events)
```

---

## 🎯 Important Notes

### Auto-Indexing Events
- **CREATE**: source = "auto_crud_create"
- **UPDATE**: source = "auto_crud_update"
- **DELETE**: source = "auto_crud_delete" (different event type)

### Highlights Field
- Takes `details.topAttractions` array
- Joins with spaces: `"A B C"`
- Empty string if missing or null
- Used in search queries

### Error Handling
- 401: Missing or invalid JWT token
- 404: Destination not found
- 500: Elasticsearch or DB connection failure
- Always log errors for debugging

### Performance Considerations
- Indexing happens synchronously (< 100ms)
- Event logging is asynchronous
- Use Elasticsearch for full-text search
- Use PostgreSQL for relational queries

---

## 📚 Related Documentation

| Document | Purpose |
|----------|---------|
| S2-F11_IMPLEMENTATION.md | Complete spec & behavior |
| S2-F11_API_EXAMPLES.md | Request/response examples |
| S2-F11_DEPLOYMENT_CHECKLIST.md | Deployment guide |
| S2-F11_COMPLETE_SUMMARY.md | Architecture & design |

---

## ❓ FAQ

**Q: Do I need to call /index endpoint?**
A: No! It's automatic on create/update. The endpoint is for explicit re-indexing.

**Q: What if topAttractions is missing?**
A: highlights field defaults to empty string.

**Q: Can I search destinations?**
A: Yes! Elasticsearch has the data for full-text search (implement search endpoint separately).

**Q: Will indexing fail if Elasticsearch is down?**
A: Yes, it will throw 500 error. This is a hard dependency.

**Q: What if MongoDB is down?**
A: Event logging will fail softly (only logs warning). Request still succeeds.

**Q: How do I verify it's working?**
A: Check Elasticsearch and MongoDB collections for documents/events.

---

## 🚀 Next Steps

1. **Understand the code**: Read through the 3 new files
2. **Review the tests**: Check DestinationSearchIntegrationTest.java
3. **Test locally**: Run the 6 test scenarios
4. **Review changes**: Check modifications in DestinationService and Controller
5. **Read documentation**: Review the 4 documentation files
6. **Prepare deployment**: Follow deployment checklist

---

**Happy Coding! 🎉**

For detailed information, refer to the comprehensive documentation files in the project root.

