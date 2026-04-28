package com.team15.tripplanning.itineraryservice.service;

import com.team15.tripplanning.itineraryservice.dto.ItineraryAnalyticsDTO;
import com.team15.tripplanning.itineraryservice.dto.TripCostEstimateDTO;
import com.team15.tripplanning.itineraryservice.model.Itinerary;
import com.team15.tripplanning.itineraryservice.repository.ItineraryRepository;
import java.util.List;
import com.team15.tripplanning.itineraryservice.dto.ItineraryDayRequestDTO;
import com.team15.tripplanning.itineraryservice.model.ItineraryDay;
import java.util.ArrayList;
import com.team15.tripplanning.itineraryservice.dto.ItineraryDayDTO;
import com.team15.tripplanning.itineraryservice.dto.ItineraryDetailsDTO;

import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItineraryService {
    private final ItineraryRepository itineraryRepository;

    public ItineraryService(ItineraryRepository itineraryRepository) {
        this.itineraryRepository = itineraryRepository;
    }

    public Itinerary create(Itinerary itinerary) {
        return itineraryRepository.save(itinerary);
    }

    public List<Itinerary> findAll() {
        return itineraryRepository.findAll();
    }

    public Itinerary findById(Long id) {
        return itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found: " + id));
    }

    public Itinerary update(Long id, Itinerary itinerary) {
        Itinerary existing = findById(id);
        existing.setUserId(itinerary.getUserId());
        existing.setDestinationId(itinerary.getDestinationId());
        existing.setTitle(itinerary.getTitle());
        existing.setStatus(itinerary.getStatus());
        existing.setEstimatedBudget(itinerary.getEstimatedBudget());
        existing.setMetadata(itinerary.getMetadata());
        existing.setStartDate(itinerary.getStartDate());
        existing.setEndDate(itinerary.getEndDate());
        return itineraryRepository.save(existing);
    }

    public void delete(Long id) {
        itineraryRepository.delete(findById(id));
    }

    // S3-F1
    public List<Itinerary> searchByStatusAndDateRange(String status, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return itineraryRepository.searchByStatusAndDateRange(status, startDate, endDate);
    }

    @Transactional
    public Itinerary assignDestination(Long itineraryId, Long destinationId) {

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Itinerary not found"));

        // Allow assignment for DRAFT and PLANNED itineraries
        if (itinerary.getStatus() != Itinerary.ItineraryStatus.DRAFT && 
            itinerary.getStatus() != Itinerary.ItineraryStatus.PLANNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Itinerary must be in DRAFT or PLANNED status to assign a destination");
        }

        // Note: Destination validation is skipped in microservices architecture
        // The destination-service owns the destinations data and validation
        // Trust that the destinationId is valid; let downstream services catch invalid destinations

        itinerary.setDestinationId(destinationId);
        // Only set status to PLANNED if currently in DRAFT
        if (itinerary.getStatus() == Itinerary.ItineraryStatus.DRAFT) {
            itinerary.setStatus(Itinerary.ItineraryStatus.PLANNED);
        }
        return itineraryRepository.save(itinerary);
    }



    // S3-F3
    public TripCostEstimateDTO estimateTripCost(Long destinationId, int numberOfDays, int numberOfTravelers) {
        double accommodation = 150.0 * numberOfDays * numberOfTravelers;
        double transport     = 50.0  * numberOfDays * numberOfTravelers;
        double activities    = 100.0 * numberOfDays;

        int activeCount = itineraryRepository.countActiveItinerariesForDestination(destinationId);

        double seasonMultiplier;
        if (activeCount <= 5) {
            seasonMultiplier = 1.0;
        } else if (activeCount <= 15) {
            seasonMultiplier = 1.3;
        } else {
            seasonMultiplier = 1.6;
        }

        double total = (accommodation + transport + activities) * seasonMultiplier;
        return new TripCostEstimateDTO(accommodation, transport, activities, total, seasonMultiplier);
    }
    @Transactional
    public Itinerary completeItinerary(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Itinerary not found"));

        if (itinerary.getStatus() != Itinerary.ItineraryStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Itinerary must be IN_PROGRESS to complete");
        }

        itinerary.setStatus(Itinerary.ItineraryStatus.COMPLETED);

        if (itinerary.getEstimatedBudget() == null) {
            Double total = itineraryRepository.sumConfirmedBookingsByItinerary(id);
            itinerary.setEstimatedBudget(total != null ? total : 0.0);
        }

        return itineraryRepository.save(itinerary);
    }

    public List<Itinerary> filterByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "key and value must not be blank");
        }
        return itineraryRepository.findByMetadataKeyValue(key, value);
    }
    @Transactional
    public Itinerary addDays(Long itineraryId, List<ItineraryDayRequestDTO> daysRequest) {

        // 🔍 1. Find itinerary
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found"));

        // ❌ 2. Status validation
        if (!(itinerary.getStatus() == Itinerary.ItineraryStatus.DRAFT ||
                itinerary.getStatus() == Itinerary.ItineraryStatus.PLANNED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot add days to in-progress or completed itineraries");
        }

        // ❌ 3. Validate input
        // ✅ Replace with:
        for (ItineraryDayRequestDTO dto : daysRequest) {
            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Day title must not be blank");
            }
            if (dto.getDate() == null) {
                dto.setDate(java.time.LocalDate.now());
            }
        }

        // 🔢 4. Get current max dayOrder
        int currentMax = 0;
        if (itinerary.getItineraryDays() != null && !itinerary.getItineraryDays().isEmpty()) {
            currentMax = itinerary.getItineraryDays()
                    .stream()
                    .mapToInt(ItineraryDay::getDayOrder)
                    .max()
                    .orElse(0);
        }

        // ➕ 5. Create new days
        List<ItineraryDay> newDays = new ArrayList<>();

        for (int i = 0; i < daysRequest.size(); i++) {
            ItineraryDayRequestDTO dto = daysRequest.get(i);

            ItineraryDay day = new ItineraryDay();
            day.setDate(dto.getDate());
            day.setTitle(dto.getTitle());
            day.setDescription(dto.getDescription());
            day.setMetadata(dto.getMetadata());

            day.setDayOrder(currentMax + i + 1);
            day.setStatus(ItineraryDay.ItineraryDayStatus.PLANNED);

            day.setItinerary(itinerary); // 🔗 relationship

            newDays.add(day);
        }

        // 🧩 6. Attach to itinerary
        if (itinerary.getItineraryDays() == null) {
            itinerary.setItineraryDays(new ArrayList<>());
        }
        itinerary.getItineraryDays().addAll(newDays);

        // 💾 7. Save
        return itineraryRepository.save(itinerary);
    }
    public ItineraryDetailsDTO getItineraryDetails(Long id) {

        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Itinerary not found: " + id
                ));

        List<ItineraryDay> days = itinerary.getItineraryDays() != null
                ? itinerary.getItineraryDays()
                : new ArrayList<>();

        // sort by dayOrder
        days.sort(Comparator.comparingInt(ItineraryDay::getDayOrder));

        List<ItineraryDayDTO> dayDTOs = new ArrayList<>();
        int completedCount = 0;

        for (ItineraryDay day : days) {
            ItineraryDayDTO dto = new ItineraryDayDTO();

            dto.setId(day.getId());
            dto.setDayOrder(day.getDayOrder());
            dto.setDate(day.getDate().toString());
            dto.setTitle(day.getTitle());
            dto.setDescription(day.getDescription());
            dto.setStatus(day.getStatus().name());
            dto.setMetadata(day.getMetadata());

            if (day.getStatus() == ItineraryDay.ItineraryDayStatus.COMPLETED) {
                completedCount++;
            }

            dayDTOs.add(dto);
        }

        ItineraryDetailsDTO response = new ItineraryDetailsDTO();

        response.setItineraryId(itinerary.getId());
        response.setUserId(itinerary.getUserId());
        response.setDestinationId(itinerary.getDestinationId());
        response.setTitle(itinerary.getTitle());
        response.setStatus(itinerary.getStatus().name());
        response.setEstimatedBudget(itinerary.getEstimatedBudget());
        response.setMetadata(itinerary.getMetadata());

        response.setDays(dayDTOs);
        response.setTotalDays(dayDTOs.size());
        response.setCompletedDays(completedCount);

        return response;
    }

    public ItineraryAnalyticsDTO getAnalytics(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<Object[]> results = itineraryRepository.getAnalytics(startDate, endDate);

        if (results.isEmpty()) {
            return new ItineraryAnalyticsDTO(0L, 0L, 0L, 0.0, 0.0, 0.0);
        }

        Object[] row = results.get(0);

        long total       = row[0] != null ? ((Number) row[0]).longValue()   : 0L;
        long completed   = row[1] != null ? ((Number) row[1]).longValue()   : 0L;
        long cancelled   = row[2] != null ? ((Number) row[2]).longValue()   : 0L;
        double totalBudget   = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        double averageBudget = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

        double completionRate = total == 0 ? 0.0 : (completed * 100.0 / total);

        return new ItineraryAnalyticsDTO(
                total,
                completed,
                cancelled,
                totalBudget,
                averageBudget,
                completionRate
        );
    }

    @Transactional
    public Itinerary cancelItinerary(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found"));

        if (itinerary.getStatus() == Itinerary.ItineraryStatus.COMPLETED
                || itinerary.getStatus() == Itinerary.ItineraryStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DRAFT or PLANNED itineraries can be cancelled");
        }

        if (itinerary.getStatus() != Itinerary.ItineraryStatus.CANCELLED) {
            itinerary.setStatus(Itinerary.ItineraryStatus.CANCELLED);
            itineraryRepository.cancelPendingBookings(id);
            itinerary = itineraryRepository.save(itinerary);
        }

        return itinerary;
    }
}
