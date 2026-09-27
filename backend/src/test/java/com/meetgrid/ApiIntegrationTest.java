package com.meetgrid;

import com.meetgrid.service.SeedService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SeedService seed;
    private static final String SEARCH = """
        {"memberIds":["1-vivek","2-riya","3-aman","4-priya"],"durationMinutes":60,"requiredCapacity":4}
        """;
    @BeforeEach void reset() { seed.reset(); }
    @Test void completeDemoRemovesTuesdayOption() throws Exception {
        mvc.perform(get("/api/members")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/rooms")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(post("/api/meeting-options/search").contentType("application/json").content(SEARCH))
            .andExpect(status().isOk()).andExpect(jsonPath("$.options.length()").value(2))
            .andExpect(jsonPath("$.options[0].dayOfWeek").value("TUESDAY"))
            .andExpect(jsonPath("$.options[0].availableRooms[0].id").value("lab-2"))
            .andExpect(jsonPath("$.options[0].rejectedRooms.length()").value(4));
        mvc.perform(post("/api/bookings").contentType("application/json").content("""
            {"roomId":"lab-2","dayOfWeek":"TUESDAY","startTime":"14:00","endTime":"15:00"}
            """)).andExpect(status().isCreated());
        mvc.perform(post("/api/meeting-options/search").contentType("application/json").content(SEARCH))
            .andExpect(status().isOk()).andExpect(jsonPath("$.options.length()").value(1))
            .andExpect(jsonPath("$.options[0].dayOfWeek").value("THURSDAY"));
    }
    @Test void availabilityUpdatePersistsAndChangesSearch() throws Exception {
        mvc.perform(put("/api/members/1-vivek/availability").contentType("application/json").content("{\"availability\":[]}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/meeting-options/search").contentType("application/json").content(SEARCH))
            .andExpect(status().isOk()).andExpect(jsonPath("$.options.length()").value(0));
    }
    @Test void invalidInputsAndDuplicateBookingsAreRejected() throws Exception {
        mvc.perform(post("/api/meeting-options/search").contentType("application/json").content(SEARCH.replace(":60",":0")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/meeting-options/search").contentType("application/json").content(SEARCH.replace(":4",":3")))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/members/1-vivek/availability").contentType("application/json").content("""
            {"availability":[{"dayOfWeek":"MONDAY","startTime":"12:00","endTime":"10:00"}]}
            """)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/bookings").contentType("application/json").content("""
            {"roomId":"discussion-a","dayOfWeek":"TUESDAY","startTime":"14:00","endTime":"15:00"}
            """)).andExpect(status().isConflict());
    }
}
