package com.team15.tripplanning.destinationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableElasticsearchRepositories(basePackages = "com.team15.tripplanning.destinationservice.repository")
public class DestinationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DestinationServiceApplication.class, args);
    }
}
