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
import static org.hamcrest.Matchers.containsInAnyOrder;

@SpringBootTest(properties={"ADMIN_EMAIL=vivekni1224@nigam","ADMIN_BOOTSTRAP_PASSWORD=local-integration-admin-password-only"}) @AutoConfigureMockMvc
class AdminFlowTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;
 @Test void rolesContentPricingAndAtomicImport() throws Exception {
  mvc.perform(get("/api/plans")).andExpect(jsonPath("$[*].monthlyPrice",containsInAnyOrder(7,12,20)));
  mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/signup").with(csrf()).contentType("application/json").content("""
   {"email":"vivekni1224@nigam","password":"untrusted-signup-password","name":"Owner","workspaceName":"Test","timezone":"UTC"}
   """)).andExpect(status().isConflict());
  var signup=mvc.perform(post("/api/auth/signup").with(csrf()).contentType("application/json").content("""
   {"email":"rbac-user@example.test","password":"regular-account-password","name":"User","workspaceName":"User workspace","timezone":"UTC","role":"ADMIN"}
   """)).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER")).andReturn();
  var user=(MockHttpSession)signup.getRequest().getSession(false);String userId=json.readTree(signup.getResponse().getContentAsString()).path("id").asText();
  mvc.perform(get("/api/admin/users").session(user)).andExpect(status().isForbidden());
  mvc.perform(put("/api/admin/site").session(user).with(csrf()).contentType("application/json").content("{\"version\":0,\"values\":{}}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/tools/import-members").session(user).with(csrf()).contentType("application/json").content("{\"names\":[\"One\",\"Two\",\"Three\",\"Four\",\"Five\",\"Six\"]}")).andExpect(status().isConflict());
  mvc.perform(get("/api/members").session(user)).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(post("/api/tools/import-members").session(user).with(csrf()).contentType("application/json").content("{\"names\":[\"One\",\"Two\"]}")).andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(2));
  mvc.perform(post("/api/tools/import-members").session(user).with(csrf()).contentType("application/json").content("{\"names\":[\"Three\",\"one\"]}")).andExpect(status().isConflict());
  mvc.perform(get("/api/tools/export").session(user)).andExpect(status().isOk()).andExpect(jsonPath("$.members.length()").value(2)).andExpect(jsonPath("$.workspace.passwordHash").doesNotExist());
  var login=mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"vivekni1224@nigam\",\"password\":\"local-integration-admin-password-only\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN")).andReturn();
  var admin=(MockHttpSession)login.getRequest().getSession(false);String adminId=json.readTree(login.getResponse().getContentAsString()).path("id").asText();
  mvc.perform(get("/api/admin/overview").session(admin)).andExpect(status().isOk());
  mvc.perform(get("/api/tools/export").session(admin)).andExpect(jsonPath("$.members.length()").value(0));
  mvc.perform(put("/api/admin/site").session(admin).with(csrf()).contentType("application/json").content("{\"version\":0,\"values\":{\"brand.companyUrl\":\"javascript:alert(1)\"}}")).andExpect(status().isBadRequest());
  mvc.perform(put("/api/admin/site").session(admin).with(csrf()).contentType("application/json").content("{\"version\":0,\"values\":{\"home.heading\":\"A changed headline\"}}")).andExpect(status().isOk());
  mvc.perform(get("/api/site")).andExpect(jsonPath("$.values['home.heading']").value("A changed headline"));
  mvc.perform(put("/api/admin/site").session(admin).with(csrf()).contentType("application/json").content("{\"version\":0,\"values\":{}}")).andExpect(status().isConflict());
  String suspend="{\"name\":\"User\",\"workspaceName\":\"User workspace\",\"timezone\":\"UTC\",\"role\":\"USER\",\"suspended\":true}";
  mvc.perform(put("/api/admin/users/"+adminId).session(admin).with(csrf()).contentType("application/json").content(suspend)).andExpect(status().isConflict());
  mvc.perform(put("/api/admin/users/"+userId).session(admin).with(csrf()).contentType("application/json").content(suspend)).andExpect(status().isOk());
  mvc.perform(get("/api/members").session(user)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/audit").session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
 }
}
