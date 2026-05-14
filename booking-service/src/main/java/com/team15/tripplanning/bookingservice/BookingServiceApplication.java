package com.team15.tripplanning.bookingservice;

import com.team15.tripplanning.contracts.feign.DestinationServiceClient;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {UserServiceClient.class, ItineraryServiceClient.class, DestinationServiceClient.class})
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

}
