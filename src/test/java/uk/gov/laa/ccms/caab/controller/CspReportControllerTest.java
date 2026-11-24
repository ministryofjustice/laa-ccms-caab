package uk.gov.laa.ccms.caab.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.laa.ccms.caab.bean.metric.PuiMetricService;

@ExtendWith(MockitoExtension.class)
class CspReportControllerTest {

  @Mock private PuiMetricService puiMetricService;

  private CspReportController controller;

  @BeforeEach
  void setUp() {
    controller = new CspReportController(puiMetricService);
  }

  @Test
  void shouldIncrementMetricAndReturnNoContentForStructuredReport() throws Exception {
    ResponseEntity<Void> response =
        report(
            """
            {
              "csp-report": {
                "document-uri": "https://example.test/home",
                "violated-directive": "script-src-elem",
                "effective-directive": "script-src-elem",
                "blocked-uri": "inline",
                "source-file": "https://example.test/home",
                "line-number": 12,
                "column-number": 3,
                "status-code": 200,
                "disposition": "report",
                "script-sample": "",
                "original-policy": "default-src 'self'"
              }
            }
            """);

    verify(puiMetricService).incrementCspViolationReportsCount();
    assertThat(response.getStatusCode().value()).isEqualTo(204);
  }

  @Test
  void shouldIncrementMetricAndReturnNoContentForMalformedReport() throws Exception {
    ResponseEntity<Void> response = report("{not-json");

    verify(puiMetricService).incrementCspViolationReportsCount();
    verifyNoMoreInteractions(puiMetricService);
    assertThat(response.getStatusCode().value()).isEqualTo(204);
  }

  @Test
  void shouldNotLogReportBodyOrQueryStrings() throws Exception {
    var logger =
        (ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(CspReportController.class);
    var appender =
        new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      report("{not-json jane.doe@example.com");
      report(
          """
          {"csp-report": {"document-uri": "https://example.test/case?ref=300001&name=Jane",
          "violated-directive": "script-src", "script-sample": "Jane Doe AB123456C"}}
          """);

      String logged =
          appender.list.stream()
              .map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
              .collect(java.util.stream.Collectors.joining("\n"));
      assertThat(logged).isNotBlank();
      assertThat(logged)
          .doesNotContain("jane.doe", "Jane", "AB123456C", "ref=300001")
          .contains("https://example.test/case");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void acceptsBoundaryMeasuredInUtf8Bytes() throws Exception {
    assertThat(report("é".repeat(8192)).getStatusCode().value()).isEqualTo(204);
    verify(puiMetricService).incrementCspViolationReportsCount();
  }

  @Test
  void rejectsDeclaredOversizeWithoutReading() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContentLengthLong()).thenReturn(16385L);
    assertThat(controller.report(request).getStatusCode().value()).isEqualTo(413);
    verify(request).getContentLengthLong();
    verifyNoMoreInteractions(request);
    verifyNoInteractions(puiMetricService);
  }

  @Test
  void rejectsUnknownLengthOversizeWithoutLoggingBody() throws Exception {
    var logger =
        (ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(CspReportController.class);
    var appender =
        new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      MockHttpServletRequest request =
          new MockHttpServletRequest() {
            @Override
            public long getContentLengthLong() {
              return -1;
            }
          };
      request.setContent(("secret" + "x".repeat(16384)).getBytes(StandardCharsets.UTF_8));
      assertThat(controller.report(request).getStatusCode().value()).isEqualTo(413);
      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.get(0).getFormattedMessage()).doesNotContain("secret");
      verifyNoInteractions(puiMetricService);
    } finally {
      logger.detachAppender(appender);
    }
  }

  private ResponseEntity<Void> report(String body) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    return controller.report(request);
  }
}
