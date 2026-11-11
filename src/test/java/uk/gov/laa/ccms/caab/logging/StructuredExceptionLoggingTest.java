package uk.gov.laa.ccms.caab.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Runs a log event through the ECS output configured in {@code application.yml}. */
class StructuredExceptionLoggingTest {

  private LoggerContext loggerContext;
  private StructuredLogEncoder encoder;

  @BeforeEach
  void setUp() throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    PropertySource<?> defaults =
        new YamlPropertySourceLoader()
                .load("application", new FileSystemResource("src/main/resources/application.yml"))
                .stream()
                .filter(source -> source.containsProperty("logging.structured.format.console"))
                .findFirst()
                .orElseThrow();
    environment.getPropertySources().addFirst(defaults);

    loggerContext = new LoggerContext();
    loggerContext.setMDCAdapter(new LogbackMDCAdapter());
    loggerContext.putObject(Environment.class.getName(), environment);
    encoder = new StructuredLogEncoder();
    encoder.setContext(loggerContext);
    encoder.setFormat(environment.getRequiredProperty("logging.structured.format.console"));
    encoder.start();
  }

  @Test
  void anApiFailureDoesNotCarryItsQueryParametersIntoTheLog() {
    var request = org.mockito.Mockito.mock(org.springframework.http.HttpRequest.class);
    org.mockito.Mockito.when(request.getMethod()).thenReturn(HttpMethod.GET);
    org.mockito.Mockito.when(request.getURI())
        .thenReturn(URI.create("https://ebs.test/clients?surname=Smith&dob=1980-02-03"));
    var failure =
        WebClientResponseException.create(
            400, "Bad Request", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8, request);

    String line = encode("Failed to retrieve clients", failure);

    assertThat(line)
        .doesNotContain("Smith", "1980-02-03", "surname=", "ebs.test")
        .contains("WebClientResponseException", "HTTP 400");
  }

  @Test
  void exceptionMessagesAreNotLoggedButTheClassesAndLocationsAre() {
    var failure =
        new IllegalStateException(
            "could not process Jane Doe AB123456C", new IllegalArgumentException("10 High Street"));

    String line = encode("Something failed", failure);

    assertThat(line)
        .doesNotContain("Jane", "AB123456C", "High Street")
        .contains(
            "IllegalStateException",
            "IllegalArgumentException",
            "Caused by",
            "StructuredExceptionLoggingTest");
  }

  @Test
  void ordinaryMessagesAreUnchanged() {
    String line = encode("Application started", null);

    assertThat(line).contains("Application started").contains("\"ecs\":{\"version\":");
  }

  private String encode(String message, Throwable throwable) {
    LoggingEvent event =
        new LoggingEvent(
            StructuredExceptionLoggingTest.class.getName(),
            loggerContext.getLogger(StructuredExceptionLoggingTest.class),
            Level.ERROR,
            message,
            throwable,
            null);
    return new String(encoder.encode(event), StandardCharsets.UTF_8);
  }
}
