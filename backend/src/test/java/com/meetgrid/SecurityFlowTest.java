package com.meetgrid;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties={"STRIPE_WEBHOOK_SECRET=integration-test-secret","STRIPE_PRICE_STUDIO=price_studio_test"})
@AutoConfigureMockMvc
class SecurityFlowTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;
 @Test void completePrivateWorkspaceAndBillingFlow() throws Exception {
   mvc.perform(get("/api/members")).andExpect(status().isUnauthorized());
   mvc.perform(post("/api/auth/signup").contentType("application/json").content("{}"))
       .andExpect(status().isForbidden());
   var first=mvc.perform(post("/api/auth/signup").with(csrf()).contentType("application/json").content("""
       {"email":"owner@example.test","password":"a-long-test-password","name":"Owner","workspaceName":"Design studio","timezone":"Asia/Kolkata"}
       """)).andExpect(status().isCreated()).andReturn();
   var session=(MockHttpSession)first.getRequest().getSession(false);
   String owner=json.readTree(first.getResponse().getContentAsString()).path("id").asText();
   mvc.perform(get("/api/members").session(session)).andExpect(jsonPath("$.length()").value(0));
   var member=mvc.perform(post("/api/members").session(session).with(csrf()).contentType("application/json").content("{\"name\":\"My collaborator\",\"color\":\"pink\"}"))
       .andExpect(status().isCreated()).andReturn();
   String memberId=json.readTree(member.getResponse().getContentAsString()).path("id").asText();
   mvc.perform(put("/api/members/"+memberId+"/availability").session(session).with(csrf()).contentType("application/json").content("""
       {"availability":[{"dayOfWeek":"SATURDAY","startTime":"10:00","endTime":"11:00"}]}
       """)).andExpect(status().isOk());
   var room=mvc.perform(post("/api/rooms").session(session).with(csrf()).contentType("application/json").content("""
       {"name":"Our room","location":"Office","capacity":4,"openTime":"09:00","closeTime":"18:00"}
       """)).andExpect(status().isCreated()).andReturn();
   String roomId=json.readTree(room.getResponse().getContentAsString()).path("id").asText();
   String search="{\"memberIds\":[\""+memberId+"\"],\"durationMinutes\":60,\"requiredCapacity\":2}";
   mvc.perform(post("/api/meeting-options/search").session(session).with(csrf()).contentType("application/json").content(search))
       .andExpect(status().isOk()).andExpect(jsonPath("$.options[0].dayOfWeek").value("SATURDAY"));
   String reservation="{\"roomId\":\""+roomId+"\",\"dayOfWeek\":\"SATURDAY\",\"startTime\":\"10:00\",\"endTime\":\"11:00\"}";
   var booking=mvc.perform(post("/api/bookings").session(session).with(csrf()).contentType("application/json").content(reservation)).andExpect(status().isCreated()).andReturn();
   String bookingId=json.readTree(booking.getResponse().getContentAsString()).path("id").asText();
   mvc.perform(post("/api/bookings").session(session).with(csrf()).contentType("application/json").content(reservation)).andExpect(status().isConflict());
   mvc.perform(post("/api/templates").session(session).with(csrf()).contentType("application/json").content("{\"name\":\"Weekly review\",\"description\":\"Our creative review\",\"durationMinutes\":45,\"capacity\":4}"))
       .andExpect(status().isCreated());
   mvc.perform(put("/api/workspace").session(session).with(csrf()).contentType("application/json").content("{\"name\":\"Owner renamed\",\"workspaceName\":\"Our studio\",\"timezone\":\"UTC\"}"))
       .andExpect(status().isOk()).andExpect(jsonPath("$.workspaceName").value("Our studio"));
   var second=mvc.perform(post("/api/auth/signup").with(csrf()).contentType("application/json").content("""
       {"email":"other@example.test","password":"another-long-password","name":"Other","workspaceName":"Other studio","timezone":"UTC"}
       """)).andExpect(status().isCreated()).andReturn();
   var other=(MockHttpSession)second.getRequest().getSession(false);
   mvc.perform(get("/api/members").session(other)).andExpect(jsonPath("$.length()").value(0));
   mvc.perform(get("/api/bookings").session(other)).andExpect(jsonPath("$.length()").value(0));
   mvc.perform(get("/api/templates").session(other)).andExpect(jsonPath("$.length()").value(0));
   mvc.perform(delete("/api/members/"+memberId).session(other).with(csrf())).andExpect(status().isNotFound());
   mvc.perform(delete("/api/bookings/"+bookingId).session(other).with(csrf())).andExpect(status().isNotFound());
   mvc.perform(post("/api/bookings").session(other).with(csrf()).contentType("application/json").content(reservation)).andExpect(status().isNotFound());
   mvc.perform(post("/api/billing/webhook").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
   String event="{\"type\":\"customer.subscription.created\",\"created\":100,\"data\":{\"object\":{\"id\":\"sub_test\",\"customer\":\"cus_test\",\"status\":\"active\",\"metadata\":{\"account_id\":\""+owner+"\"},\"items\":{\"data\":[{\"price\":{\"id\":\"price_studio_test\"}}]}}}}";
   String timestamp=String.valueOf(java.time.Instant.now().getEpochSecond());var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec("integration-test-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));
   String signature=java.util.HexFormat.of().formatHex(mac.doFinal((timestamp+"."+event).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
   mvc.perform(post("/api/billing/webhook").header("Stripe-Signature","t="+timestamp+",v1="+signature).contentType("application/json").content(event)).andExpect(status().isOk());
   mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.plan").value("studio"));
   mvc.perform(get("/api/auth/me").session(other)).andExpect(jsonPath("$.plan").value("free"));
   mvc.perform(delete("/api/bookings/"+bookingId).session(session).with(csrf())).andExpect(status().isNoContent());
   mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
   mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"owner@example.test\",\"password\":\"wrong-password\"}")).andExpect(status().isUnauthorized());
   mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"owner@example.test\",\"password\":\"a-long-test-password\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.workspaceName").value("Our studio"));
 }
}
