package com.team15.tripplanning.destinationservice;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team15.tripplanning.destinationservice.controller.DestinationController;
import com.team15.tripplanning.destinationservice.dto.DestinationRevenueDTO;
import com.team15.tripplanning.destinationservice.dto.DestinationReviewAlertDTO;
import com.team15.tripplanning.destinationservice.dto.TopDestinationDTO;
import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.service.DestinationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DestinationReportsIntegrationTest {

    private MockMvc mockMvc;
    private DestinationService destinationService;

    @BeforeEach
    void setUp() {
        destinationService = Mockito.mock(DestinationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DestinationController(destinationService)).build();
    }

    @Test
    void getRevenueReportReturnsPopulatedDto() throws Exception {
        DestinationRevenueDTO dto = DestinationRevenueDTO.builder()
                .destinationId(11L)
                .name("Cairo")
                .totalBookings(9L)
                .totalRevenue(1320.5)
                .averageBookingAmount(146.7)
                .build();

        when(destinationService.getDestinationRevenueSummary(eq(11L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(dto);

        mockMvc.perform(get("/api/destinations/11/revenue")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinationId", is(11)))
                .andExpect(jsonPath("$.name", is("Cairo")))
                .andExpect(jsonPath("$.totalBookings", is(9)))
                .andExpect(jsonPath("$.totalRevenue", is(1320.5)))
                .andExpect(jsonPath("$.averageBookingAmount", is(146.7)));
    }

    @Test
    void getTopRatedReturnsPopulatedList() throws Exception {
        TopDestinationDTO dto = TopDestinationDTO.builder()
                .destinationId(3L)
                .name("Lisbon")
                .rating(4.8)
                .totalBookings(22L)
                .build();

        when(destinationService.getTopRatedDestinations(5)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/destinations/reports/top-rated").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].destinationId", is(3)))
                .andExpect(jsonPath("$[0].name", is("Lisbon")))
                .andExpect(jsonPath("$[0].rating", is(4.8)))
                .andExpect(jsonPath("$[0].totalBookings", is(22)));
    }

    @Test
    void getLowRatedReturnsPopulatedList() throws Exception {
        DestinationReview review = new DestinationReview();
        review.setRating(1);

        DestinationReviewAlertDTO dto = DestinationReviewAlertDTO.builder()
                .destinationId(8L)
                .destinationName("Athens")
                .destinationStatus(DestinationStatus.ACTIVE)
                .lowRatedReviews(List.of(review))
                .build();

        when(destinationService.getLowRatedReviewAlerts(2.0)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/destinations/reviews/low-rated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].destinationId", is(8)))
                .andExpect(jsonPath("$[0].destinationName", is("Athens")))
                .andExpect(jsonPath("$[0].destinationStatus", is("ACTIVE")))
                .andExpect(jsonPath("$[0].lowRatedCount", is(1)));
    }
}

