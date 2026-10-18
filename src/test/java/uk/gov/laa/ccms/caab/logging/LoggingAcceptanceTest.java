package uk.gov.laa.ccms.caab.logging;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.brave.autoconfigure.BraveAutoConfiguration;
import org.springframework.boot.reactor.autoconfigure.ReactorAutoConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

/** Exercises the real logging lifecycle and HTTP tracing with the processed production YAML. */
@ExtendWith(OutputCaptureExtension.class)
class LoggingAcceptanceTest {
  private boolean contextPropagationEnabled;

  @BeforeEach
  void rememberReactorHook() {
    contextPropagationEnabled = Hooks.isAutomaticContextPropagationEnabled();
  }

  @AfterEach
  void restoreReactorHook() {
    if (!contextPropagationEnabled) {
      Hooks.disableAutomaticContextPropagation();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"default", "dev", "local"})
  void verifiesLoggingForRequest(String profile, CapturedOutput output) throws Exception {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    // Reproduce an earlier Spring test context having initialized the shared Logback context.
    logging.cleanUp();
    logging.beforeInitialize();
    logging.initialize(new LoggingInitializationContext(new StandardEnvironment()), null, null);
    // Each probe must initialize logging from its own production YAML, not reuse that context.
    logging.cleanUp();
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
        for (String endpoint : new String[] {"async", "blocking"}) {
          var asyncResponse =
              client.send(
                  HttpRequest.newBuilder(
                          URI.create(
                              "http://localhost:"
                                  + context.getWebServer().getPort()
                                  + "/logging-"
                                  + endpoint))
                      .GET()
                      .build(),
                  HttpResponse.BodyHandlers.ofString());
          assertEquals(200, asyncResponse.statusCode(), asyncResponse.body());
          String id = asyncResponse.body();
          assertTrue(id.matches("[0-9a-f-]{36}"), id);
          for (String stage : new String[] {"START", "CALLBACK"}) {
            String callbackLine =
                output
                    .getOut()
                    .lines()
                    .filter(l -> l.contains("LOGGING_" + endpoint.toUpperCase() + "_" + stage))
                    .findFirst()
                    .orElseThrow();
            assertTrue(callbackLine.contains(id), callbackLine);
          }
          if (endpoint.equals("async")) {
            String redispatchLine =
                output
                    .getOut()
                    .lines()
                    .filter(l -> l.contains("LOGGING_ASYNC_REDISPATCH"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(redispatchLine.contains(id), redispatchLine);
          }
        }
        var errorResponse =
            client.send(
                HttpRequest.newBuilder(
                        URI.create(
                            "http://localhost:"
                                + context.getWebServer().getPort()
                                + "/logging-error"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(500, errorResponse.statusCode());
        String errorId = errorResponse.body();
        assertTrue(errorId.matches("[0-9a-f-]{36}"), errorId);
        for (String marker : new String[] {"LOGGING_ERROR_START", "LOGGING_ERROR_HANDLER"}) {
          String errorLine =
              output.getOut().lines().filter(l -> l.contains(marker)).findFirst().orElseThrow();
          assertTrue(errorLine.contains(errorId), errorLine);
        }
        var scopeProbe = context.getBean(DispatchScopeProbe.class);
        assertTrue(scopeProbe.errorComplete.await(5, TimeUnit.SECONDS));
        assertEquals(2, scopeProbe.scopes.size(), scopeProbe.scopes.toString());
        assertTrue(
            scopeProbe.scopes.stream()
                .anyMatch(
                    scope ->
                        scope.dispatcher() == DispatcherType.REQUEST
                            && scope.path().equals("/logging-error")));
        assertTrue(
            scopeProbe.scopes.stream()
                .anyMatch(
                    scope ->
                        scope.dispatcher() == DispatcherType.ERROR
                            && scope.path().equals("/error")));
        for (var scope : scopeProbe.scopes) {
          assertNull(scope.before(), scope.toString());
          assertNull(scope.after(), scope.toString());
        }
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

  @Import(SessionCorrelationConfiguration.class)
  @TestConfiguration(proxyBeanMethods = false)
  @ImportAutoConfiguration({
    TomcatServletWebServerAutoConfiguration.class,
    DispatcherServletAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    ObservationAutoConfiguration.class,
    BraveAutoConfiguration.class,
    MicrometerTracingAutoConfiguration.class,
    WebMvcObservationAutoConfiguration.class,
    ReactorAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class
  })
  static class ProbeConfiguration {
    @Bean
    SessionCorrelationFilter sessionCorrelationFilter() {
      return new SessionCorrelationFilter();
    }

    @Bean
    SessionCorrelationContext sessionCorrelationContext() {
      return new SessionCorrelationContext();
    }

    @Bean
    AsyncDispatchProbeFilter asyncDispatchProbeFilter() {
      return new AsyncDispatchProbeFilter();
    }

    @Bean
    DispatchScopeProbe dispatchScopeProbe() {
      return new DispatchScopeProbe();
    }

    @Bean
    FilterRegistrationBean<Filter> scopeProbeRegistration(DispatchScopeProbe probe) {
      FilterRegistrationBean<Filter> registration =
          new FilterRegistrationBean<>(
              (request, response, chain) -> {
                var http = (HttpServletRequest) request;
                var dispatcher = http.getDispatcherType();
                String path = http.getRequestURI();
                String before = MDC.get("sessionID");
                try {
                  chain.doFilter(request, response);
                } finally {
                  if (path.equals("/logging-error") || dispatcher == DispatcherType.ERROR) {
                    probe.scopes.add(
                        new DispatchScope(path, dispatcher, before, MDC.get("sessionID")));
                    if (dispatcher == DispatcherType.ERROR) {
                      probe.errorComplete.countDown();
                    }
                  }
                }
              });
      registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
      registration.setDispatcherTypes(
          DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
      registration.setAsyncSupported(true);
      return registration;
    }

    @Bean
    ProbeController probeController() {
      return new ProbeController();
    }
  }

  record DispatchScope(String path, DispatcherType dispatcher, String before, String after) {}

  static class DispatchScopeProbe {
    final ConcurrentLinkedQueue<DispatchScope> scopes = new ConcurrentLinkedQueue<>();
    final CountDownLatch errorComplete = new CountDownLatch(1);
  }

  static class AsyncDispatchProbeFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
      return false;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
      if (request.getDispatcherType() == DispatcherType.ASYNC) {
        LoggerFactory.getLogger("uk.gov.laa.ccms.caab.logging.Probe")
            .info("LOGGING_ASYNC_REDISPATCH");
      }
      chain.doFilter(request, response);
    }
  }

  @TestComponent
  @RestController
  static class ProbeController implements ErrorController {
    @GetMapping("/logging-error")
    void triggerError(HttpServletResponse response) throws IOException {
      LoggerFactory.getLogger("uk.gov.laa.ccms.caab.logging.Probe").info("LOGGING_ERROR_START");
      response.sendError(500, "synthetic container error");
    }

    @RequestMapping("/error")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    String handleError(HttpServletRequest request) {
      assertEquals(DispatcherType.ERROR, request.getDispatcherType());
      LoggerFactory.getLogger("uk.gov.laa.ccms.caab.logging.Probe").info("LOGGING_ERROR_HANDLER");
      return String.valueOf(MDC.get("sessionID"));
    }

    @GetMapping("/logging-async")
    Mono<String> asyncProbe() {
      return completion("ASYNC");
    }

    @GetMapping("/logging-blocking")
    String blockingProbe() {
      return completion("BLOCKING").block();
    }

    private Mono<String> completion(String kind) {
      String id = MDC.get("sessionID");
      LoggerFactory.getLogger("uk.gov.laa.ccms.caab.logging.Probe").info("LOGGING_{}_START", kind);
      return Mono.delay(Duration.ofMillis(20))
          .map(tick -> id)
          .doOnSuccess(
              value ->
                  LoggerFactory.getLogger("uk.gov.laa.ccms.caab.logging.Probe")
                      .info("LOGGING_{}_CALLBACK", kind));
    }

    @GetMapping("/logging-probe")
    String probe() {
      LoggerFactory.getLogger("uk.gov.laa.ccms.caab.logging.Probe")
          .info("LOGGING_ACCEPTANCE_PROBE");
      return "ok";
    }
  }
}
