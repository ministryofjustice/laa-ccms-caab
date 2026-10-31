package uk.gov.laa.ccms.caab.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
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
  void shouldIncrementMetricAndReturnNoContentForStructuredReport() {
    ResponseEntity<Void> response =
        controller.report(
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
  void shouldIncrementMetricAndReturnNoContentForMalformedReport() {
    ResponseEntity<Void> response = controller.report("{not-json");

    verify(puiMetricService).incrementCspViolationReportsCount();
    verifyNoMoreInteractions(puiMetricService);
    assertThat(response.getStatusCode().value()).isEqualTo(204);
  }

  @Test
  void shouldNotLogReportBodyOrQueryStrings() {
    var logger =
        (ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(CspReportController.class);
    var appender =
        new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      controller.report("{not-json jane.doe@example.com");
      controller.report(
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
}
