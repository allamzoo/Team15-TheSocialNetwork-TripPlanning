package com.team15.tripplanning.itineraryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.team15.tripplanning.contracts.feign.BookingServiceClient;
import com.team15.tripplanning.contracts.feign.DestinationServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableFeignClients(clients = {UserServiceClient.class, DestinationServiceClient.class, BookingServiceClient.class})
public class ItineraryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItineraryServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}