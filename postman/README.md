# Postman verification for S1-F4

## Import
1. Import `S1-F4-UserDeactivation.postman_collection.json`
2. Import `Local.postman_environment.json`
3. Select the `Local Trip Planning` environment

## Prerequisites
- `user-service` running at `{{userBaseUrl}}`
- `itinerary-service` running at `{{itineraryBaseUrl}}`
- PostgreSQL running and reachable by both services

## Run order
1. `Create User (ACTIVE)`
2. `Create Itinerary (DRAFT)`
3. `Deactivate User With Active Itinerary (expect 400)`
4. `Complete Itinerary`
5. `Deactivate User After Completing Itinerary (expect 200)`
6. `Get User By Id`

## Expected results
- Step 3 returns `400 BAD_REQUEST`
- Step 5 returns `200 OK`
- Step 6 shows `status = DEACTIVATED`

## Notes
- If you run services locally on custom ports, update `userBaseUrl` and `itineraryBaseUrl` in the environment.
- The collection uses saved variables `userId` and `itineraryId` from earlier requests.

