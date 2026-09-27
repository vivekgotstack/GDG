package com.meetgrid.config;
import com.meetgrid.service.SeedService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;

@Configuration
public class SeedConfiguration {
    @Bean ApplicationRunner seedDatabase(SeedService seed) { return args -> seed.ensureSeeded(); }
}
