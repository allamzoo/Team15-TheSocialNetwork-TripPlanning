package com.team15.tripplanning.userservice;

import com.team15.tripplanning.userservice.config.TestInfrastructureConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestInfrastructureConfig.class)
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
