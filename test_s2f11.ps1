# Test S2-F11 Destination Search Indexing

# Step 1: Register a user
Write-Host "Step 1: Registering a test user..." -ForegroundColor Cyan
$registerBody = @{
    name = "Test User"
    email = "testuser@example.com"
    password = "password123"
    phone = "1234567890"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody
    Write-Host "✓ User registered successfully" -ForegroundColor Green
    $token = $registerResponse.token
    Write-Host "Token: $($token.Substring(0, 50))..." -ForegroundColor Yellow
} catch {
    Write-Host "✗ Registration failed: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Create a destination with topAttractions
Write-Host "`nStep 2: Creating a destination..." -ForegroundColor Cyan
$destinationBody = @{
    name = "Santorini"
    country = "Greece"
    description = "idyllic Greek island with caldera views"
    category = "BEACH"
    status = "ACTIVE"
    rating = 4.8
    totalRatings = 250
    details = @{
        topAttractions = @("Oia sunset", "Red Beach")
    }
} | ConvertTo-Json -Depth 10

try {
    $destResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/destinations" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $token"} `
        -Body $destinationBody
    Write-Host "✓ Destination created successfully" -ForegroundColor Green
    $destinationId = $destResponse.id
    Write-Host "Destination ID: $destinationId" -ForegroundColor Yellow
} catch {
    Write-Host "✗ Destination creation failed: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Test explicit indexing endpoint
Write-Host "`nStep 3: Explicitly indexing the destination..." -ForegroundColor Cyan
try {
    $indexResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/destinations/$destinationId/index" `
        -Method POST `
        -Headers @{"Authorization" = "Bearer $token"}
    Write-Host "✓ Destination indexed successfully" -ForegroundColor Green
} catch {
    Write-Host "✗ Indexing failed: $_" -ForegroundColor Red
    exit 1
}

# Step 4: Verify in Elasticsearch
Write-Host "`nStep 4: Verifying Elasticsearch document..." -ForegroundColor Cyan
try {
    $esResponse = Invoke-RestMethod -Uri "http://localhost:9200/destinations/_doc/$destinationId" `
        -Method GET
    Write-Host "✓ Document found in Elasticsearch" -ForegroundColor Green
    Write-Host "Name: $($esResponse._source.name)" -ForegroundColor Yellow
    Write-Host "Highlights: $($esResponse._source.highlights)" -ForegroundColor Yellow
} catch {
    Write-Host "✗ Elasticsearch verification failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`nAll tests passed!" -ForegroundColor Green


