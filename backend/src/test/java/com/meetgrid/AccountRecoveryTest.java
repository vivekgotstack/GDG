package com.meetgrid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest @AutoConfigureMockMvc
class AccountRecoveryTest extends PostgresTestSupport {
 @Autowired MockMvc mvc;
 @Test void verificationResetAndSessionRevocation()throws Exception{
  var signup=mvc.perform(post("/api/auth/signup").with(csrf()).contentType("application/json").content("""
   {"email":"recovery@example.test","password":"original-long-password","name":"Recovery","workspaceName":"Private team","timezone":"UTC"}
   """)).andExpect(status().isCreated()).andExpect(jsonPath("$.emailVerified").value(false)).andReturn();
  var session=(MockHttpSession)signup.getRequest().getSession(false);
  mvc.perform(get("/api/members").session(session)).andExpect(status().isForbidden());
  var verification=ArgumentCaptor.forClass(String.class);verify(mail).queue(eq("recovery@example.test"),anyString(),eq("VERIFY"),verification.capture(),any());
  String verifyBody="{\"token\":\""+verification.getValue()+"\"}";
  mvc.perform(post("/api/auth/verify-email").with(csrf()).contentType("application/json").content(verifyBody)).andExpect(status().isOk());
  mvc.perform(post("/api/auth/verify-email").with(csrf()).contentType("application/json").content(verifyBody)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/members").session(session)).andExpect(status().isOk());
  mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType("application/json").content("{\"email\":\"recovery@example.test\"}")).andExpect(status().isOk());
  var reset=ArgumentCaptor.forClass(String.class);verify(mail).queue(eq("recovery@example.test"),anyString(),eq("RESET"),reset.capture(),any());
  String resetBody="{\"token\":\""+reset.getValue()+"\",\"password\":\"replacement-long-password\"}";
  mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType("application/json").content(resetBody)).andExpect(status().isOk());
  mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType("application/json").content(resetBody)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"recovery@example.test\",\"password\":\"original-long-password\"}")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"recovery@example.test\",\"password\":\"replacement-long-password\"}")).andExpect(status().isOk());
 }
}
