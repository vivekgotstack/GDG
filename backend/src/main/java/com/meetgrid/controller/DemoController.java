package com.meetgrid.controller;
import com.meetgrid.service.SeedService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = "meetgrid.demo-reset-enabled", havingValue = "true")
public class DemoController {
    private final SeedService seed;
    public DemoController(SeedService seed) { this.seed = seed; }
    @PostMapping("/reset") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() { seed.reset(); }
}
