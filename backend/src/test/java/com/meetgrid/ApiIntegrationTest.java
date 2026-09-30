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
@AutoConfigureMockMvc(addFilters = false)
class ApiIntegrationTest extends PostgresTestSupport {
    @Autowired MockMvc mvc;
    @Autowired SeedService seed;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    private static final String SEARCH = """
        {"memberIds":["1-vivek","2-riya","3-aman","4-priya"],"durationMinutes":60,"requiredCapacity":4}
        """;
    @BeforeEach void reset() { seed.reset(); }
    @Test void customMemberCanBeRenamedScheduledAndRemoved() throws Exception {
        String response = mvc.perform(post("/api/members").contentType("application/json")
            .content("{\"name\":\"  Custom teammate  \",\"color\":\"pink\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Custom teammate"))
            .andExpect(jsonPath("$.availability.length()").value(0)).andReturn().getResponse().getContentAsString();
        String id = json.readTree(response).get("id").asText();
        mvc.perform(put("/api/members/"+id+"/availability").contentType("application/json").content("""
            {"availability":[{"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"10:00"}]}
            """)).andExpect(status().isOk());
        mvc.perform(put("/api/members/"+id).contentType("application/json").content("{\"name\":\"Renamed teammate\",\"color\":\"blue\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.availability.length()").value(1))
            .andExpect(jsonPath("$.name").value("Renamed teammate"));
        mvc.perform(post("/api/meeting-options/search").contentType("application/json")
            .content("{\"memberIds\":[\""+id+"\"],\"durationMinutes\":60,\"requiredCapacity\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.options[0].dayOfWeek").value("MONDAY"));
        mvc.perform(delete("/api/members/"+id)).andExpect(status().isNoContent());
        mvc.perform(put("/api/members/"+id).contentType("application/json").content("{\"name\":\"Gone\",\"color\":\"blue\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/members")).andExpect(jsonPath("$.length()").value(4));
    }
    @Test void customRoomProtectsBookingsAndCancellationReopensTheSlot() throws Exception {
        String roomInput = """
            {"name":"Team studio","capacity":7,"location":"Our office","openTime":"09:00","closeTime":"18:00"}
            """;
        String roomResponse = mvc.perform(post("/api/rooms").contentType("application/json").content(roomInput))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String roomId = json.readTree(roomResponse).get("id").asText();
        String booking = "{\"roomId\":\""+roomId+"\",\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"10:00\"}";
        String booked = mvc.perform(post("/api/bookings").contentType("application/json").content(booking))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String bookingId = json.readTree(booked).get("id").asText();
        mvc.perform(put("/api/rooms/"+roomId).contentType("application/json").content(roomInput.replace("09:00","10:00")))
            .andExpect(status().isConflict());
        mvc.perform(delete("/api/bookings/"+bookingId)).andExpect(status().isNoContent());
        mvc.perform(post("/api/bookings").contentType("application/json").content(booking)).andExpect(status().isCreated());
        mvc.perform(put("/api/rooms/"+roomId).contentType("application/json").content(roomInput.replace("Team studio","Renamed studio")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed studio"));
        mvc.perform(delete("/api/rooms/"+roomId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/bookings")).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/rooms")).andExpect(jsonPath("$.length()").value(5));
    }
    @Test void invalidCustomDetailsAreRejected() throws Exception {
        mvc.perform(post("/api/members").contentType("application/json").content("{\"name\":\"  \",\"color\":\"blue\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/members").contentType("application/json").content("{\"name\":\"Someone\",\"color\":\"invalid\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/rooms").contentType("application/json").content("""
            {"name":"Test","capacity":4,"location":"Test","openTime":"18:00","closeTime":"09:00"}
            """)).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/rooms/missing")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/bookings/missing")).andExpect(status().isNotFound());
    }
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
