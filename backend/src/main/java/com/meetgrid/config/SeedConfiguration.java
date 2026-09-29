package com.meetgrid.config;
import com.meetgrid.service.SeedService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "meetgrid.sample-data", havingValue = "true")
public class SeedConfiguration {
    @Bean ApplicationRunner seedDatabase(SeedService seed) { return args -> seed.ensureSeeded(); }
}
