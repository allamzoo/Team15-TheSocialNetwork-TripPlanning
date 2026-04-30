package com.team15.tripplanning.userservice;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team15.tripplanning.userservice.controller.UserController;
import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
import com.team15.tripplanning.userservice.service.SavedDestinationService;
import com.team15.tripplanning.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserTripSummaryEndpointIntegrationTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        SavedDestinationService savedDestinationService = Mockito.mock(SavedDestinationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, savedDestinationService)).build();
    }

    @Test
    void tripSummaryEndpointReturnsExpectedFields() throws Exception {
        UserTripSummaryDTO dto = UserTripSummaryDTO.builder()
                .userId(15L)
                .name("Nora")
                .totalTrips(10L)
                .completedTrips(7L)
                .cancelledTrips(3L)
                .totalSpent(1550.0)
                .averageBudget(155.0)
                .build();

        when(userService.getTripSummary(15L)).thenReturn(dto);

        mockMvc.perform(get("/api/users/15/trip-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(15)))
                .andExpect(jsonPath("$.name", is("Nora")))
                .andExpect(jsonPath("$.totalTrips", is(10)))
                .andExpect(jsonPath("$.completedTrips", is(7)))
                .andExpect(jsonPath("$.cancelledTrips", is(3)))
                .andExpect(jsonPath("$.totalSpent", is(1550.0)))
                .andExpect(jsonPath("$.averageBudget", is(155.0)));
    }
}

