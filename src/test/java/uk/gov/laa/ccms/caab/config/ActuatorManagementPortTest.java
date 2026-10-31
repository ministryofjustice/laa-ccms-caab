package uk.gov.laa.ccms.caab.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
    classes = ActuatorManagementPortTest.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "management.server.port=0",
      "management.endpoints.web.exposure.include=health,prometheus",
      "server.servlet.context-path=/civil"
    })
class ActuatorManagementPortTest {

  @Value("${local.server.port}")
  private int port;

  @Value("${local.management.port}")
  private int managementPort;

  @Test
  void actuatorIsServedWithoutLoginOnTheManagementPort() throws Exception {
    assertThat(status(managementPort, "/actuator/health")).isEqualTo(200);
    assertThat(status(managementPort, "/actuator/prometheus")).isEqualTo(200);
  }

  @Test
  void actuatorIsNotServedOnTheApplicationPort() throws Exception {
    assertThat(managementPort).isNotEqualTo(port);
    assertThat(status(port, "/civil/actuator/health")).isNotEqualTo(200);
    assertThat(status(port, "/civil/actuator/prometheus")).isNotEqualTo(200);
  }

  @Test
  void theApplicationPortStillRequiresLogin() throws Exception {
    assertThat(status(port, "/civil/ping")).isIn(401, 403);
  }

  @Test
  void theApplicationConfigurationMovesActuatorToItsOwnPort() throws Exception {
    var sources =
        new org.springframework.boot.env.YamlPropertySourceLoader()
            .load("application", new FileSystemResource("src/main/resources/application.yml"));

    assertThat(sources.getFirst().getProperty("management.server.port"))
        .isEqualTo("${MANAGEMENT_PORT:8082}");
  }

  private static int status(int port, String path) throws Exception {
    try (HttpClient client = HttpClient.newHttpClient()) {
      return client
          .send(
              HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
              HttpResponse.BodyHandlers.discarding())
          .statusCode();
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class TestApplication {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      return http.authorizeHttpRequests(
              authorize ->
                  authorize
                      .requestMatchers(
                          HttpMethod.GET, "/actuator/prometheus", "/actuator/health/**")
                      .permitAll()
                      .anyRequest()
                      .authenticated())
          .build();
    }

    @RestController
    static class PingController {
      @GetMapping("/ping")
      String ping() {
        return "pong";
      }
    }
  }
}
