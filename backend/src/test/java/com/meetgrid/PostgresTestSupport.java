package com.meetgrid;
import com.meetgrid.repository.AccountRepository;
import com.meetgrid.service.MailOutbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.containers.*;
@Testcontainers(disabledWithoutDocker=true)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
abstract class PostgresTestSupport {
 @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:17-alpine");
 @Container static final GenericContainer<?> REDIS=new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);
 @MockitoBean MailOutbox mail;
 @Autowired AccountRepository testAccounts;
 @DynamicPropertySource static void configure(DynamicPropertyRegistry r){
  r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
  r.add("spring.data.redis.host",REDIS::getHost);r.add("spring.data.redis.port",()->REDIS.getMappedPort(6379));r.add("spring.data.redis.password",()->"");
  // MockMvc uses MockHttpSession. Production Redis sessions are enabled in application.yaml.
  r.add("spring.autoconfigure.exclude",()->"org.springframework.boot.autoconfigure.session.SessionAutoConfiguration");
 }
 void verifyFixture(String email){var a=testAccounts.findByEmail(email).orElseThrow();a.emailVerified=true;testAccounts.saveAndFlush(a);}
}
