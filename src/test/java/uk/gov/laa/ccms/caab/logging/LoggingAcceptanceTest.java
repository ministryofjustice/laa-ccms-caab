package uk.gov.laa.ccms.caab.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.brave.autoconfigure.BraveAutoConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exercises the real logging lifecycle and HTTP tracing with the processed production YAML. */
@ExtendWith(OutputCaptureExtension.class)
class LoggingAcceptanceTest {

  @ParameterizedTest
  @ValueSource(strings = {"default", "dev", "local"})
  void verifiesLoggingForRequest(String profile, CapturedOutput output) throws Exception {
    StandardEnvironment env = new StandardEnvironment();
    env.getPropertySources()
        .addFirst(
            new SystemEnvironmentPropertySource(
                "logging-test-env",
                Map.of(
                    "ROOT_LOGGING_LEVEL",
                    "WARN",
                    "SPRING_LOGGING_LEVEL",
                    "ERROR",
                    "APP_LOGGING_LEVEL",
                    "INFO",
                    "HOSTNAME",
                    "logging-test-node")));
    SpringApplication app = new SpringApplication(ProbeConfiguration.class);
    app.setEnvironment(env);
    java.util.List<String> args =
        new java.util.ArrayList<>(
            java.util.List.of(
                "--spring.config.location=file:"
                    + Path.of("build/resources/main/application.yml").toAbsolutePath(),
                "--server.port=0",
                "--server.servlet.context-path=/",
                "--spring.main.banner-mode=off"));
    if (!profile.equals("default")) {
      args.add("--spring.profiles.active=" + profile);
    }
    try (var context = (ServletWebServerApplicationContext) app.run(args.toArray(String[]::new))) {
      String version = context.getEnvironment().getRequiredProperty("spring.application.version");
      assertFalse(version.contains("@"));
      assertNotEquals("unspecified", version);
      LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
      assertEquals(LogLevel.WARN, logging.getLoggerConfiguration("ROOT").getEffectiveLevel());
      assertEquals(
          LogLevel.ERROR,
          logging.getLoggerConfiguration("org.springframework").getEffectiveLevel());
      assertEquals(
          LogLevel.INFO,
          logging.getLoggerConfiguration("uk.gov.laa.ccms.caab").getEffectiveLevel());
      try (HttpClient client = HttpClient.newHttpClient()) {
        var response =
            client.send(
                HttpRequest.newBuilder(
                        URI.create(
                            "http://localhost:"
                                + context.getWebServer().getPort()
                                + "/logging-probe"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
      }
      String line =
          output
              .getOut()
              .lines()
              .filter(s -> s.contains("LOGGING_ACCEPTANCE_PROBE"))
              .findFirst()
              .orElseThrow();
      if (profile.equals("local")) {
        assertFalse(line.startsWith("{"));
        assertTrue(line.matches(".*\\[[0-9a-f]{16,32},[0-9a-f]{16},[0-9a-f-]{36}\\].*"), line);
      } else {
        assertTrue(line.startsWith("{"), line);
        for (String key :
            new String[] {
              "@timestamp", "log", "level", "message", "service", "version", "environment", "node"
            }) {
          assertTrue(line.contains("\"" + key + "\":"), key + " missing: " + line);
        }
        assertTrue(line.contains("\"name\":\"laa-ccms-caab\""), line);
        assertTrue(line.contains("\"version\":\"" + version + "\""));
        assertTrue(line.contains("\"environment\":\"" + profile + "\""));
        assertTrue(line.contains("\"name\":\"logging-test-node\""));
        assertTrue(line.matches(".*\"traceId\":\"[0-9a-f]{16,32}\".*"), line);
        assertTrue(line.matches(".*\"spanId\":\"[0-9a-f]{16}\".*"), line);
        assertTrue(line.matches(".*\"sessionID\":\"[0-9a-f-]{36}\".*"), line);
      }

      Path evidence = Path.of("build/logging-evidence/" + profile + ".log");
      Files.createDirectories(evidence.getParent());
      Files.writeString(evidence, line + System.lineSeparator());
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  @ImportAutoConfiguration({
    TomcatServletWebServerAutoConfiguration.class,
    DispatcherServletAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    ObservationAutoConfiguration.class,
    BraveAutoConfiguration.class,
    MicrometerTracingAutoConfiguration.class,
    WebMvcObservationAutoConfiguration.class
  })
  static class ProbeConfiguration {
    @Bean
    SessionCorrelationFilter sessionCorrelationFilter() {
      return new SessionCorrelationFilter();
    }

    @Bean
    ProbeController probeController() {
      return new ProbeController();
    }
  }

  @TestComponent
  @RestController
  static class ProbeController {
    @GetMapping("/logging-probe")
    String probe() {
      LoggerFactory.getLogger("uk.gov.laa.ccms.caab.logging.Probe")
          .info("LOGGING_ACCEPTANCE_PROBE");
      return "ok";
    }
  }
}
