package com.team15.tripplanning.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.DestinationServiceClient;

@SpringBootApplication
@EnableFeignClients(clients = {UserServiceClient.class, ItineraryServiceClient.class, DestinationServiceClient.class})
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

}
