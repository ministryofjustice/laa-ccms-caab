package uk.gov.laa.ccms.caab.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.laa.ccms.caab.bean.metric.PuiMetricService;
import uk.gov.laa.ccms.caab.logging.LogMasker;
import uk.gov.laa.ccms.caab.model.csp.CspReport;

/** Receives browser Content Security Policy violation reports. */
@RestController
@Slf4j
@ConditionalOnProperty(name = "csp.report-enabled", havingValue = "true", matchIfMissing = true)
public class CspReportController {

  private static final int MAX_REPORT_BYTES = 16384;
  private final PuiMetricService puiMetricService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public CspReportController(PuiMetricService puiMetricService) {
    this.puiMetricService = puiMetricService;
  }

  /**
   * Receives and logs a browser CSP violation report.
   *
   * @param request the incoming CSP report
   * @return an empty 204 response, or 413 if the report exceeds the limit
   */
  @PostMapping("/csp/report")
  public ResponseEntity<Void> report(HttpServletRequest request) throws IOException {
    long length = request.getContentLengthLong();
    if (length > MAX_REPORT_BYTES) {
      log.warn("CSP report rejected, length {}", length);
      return ResponseEntity.status(413).build();
    }
    byte[] body = request.getInputStream().readNBytes(MAX_REPORT_BYTES + 1);
    if (body.length > MAX_REPORT_BYTES) {
      log.warn("CSP report rejected, length {}", body.length);
      return ResponseEntity.status(413).build();
    }
    log.debug("CSP report endpoint called");
    puiMetricService.incrementCspViolationReportsCount();
    logReport(new String(body, StandardCharsets.UTF_8));
    return ResponseEntity.noContent().build();
  }

  private void logReport(String reportBody) {
    if (reportBody == null || reportBody.isBlank()) {
      log.warn("CSP violation report received with empty body");
      return;
    }

    try {
      CspReport report = objectMapper.readValue(reportBody, CspReport.class);
      CspReport.Violation violation = report.getViolation();
      if (violation == null) {
        log.warn(
            "CSP violation report received without recognised body, length {}",
            reportBody.length());
        return;
      }

      log.warn(
          "CSP violation: effectiveDirective=[{}], violatedDirective=[{}], blockedUri=[{}], "
              + "documentUri=[{}], sourceFile=[{}], lineNumber=[{}], columnNumber=[{}], "
              + "statusCode=[{}], disposition=[{}], originalPolicy=[{}]",
          violation.getEffectiveDirective(),
          violation.getViolatedDirective(),
          LogMasker.withoutQuery(violation.getBlockedUri()),
          LogMasker.withoutQuery(violation.getDocumentUri()),
          LogMasker.withoutQuery(violation.getSourceFile()),
          violation.getLineNumber(),
          violation.getColumnNumber(),
          violation.getStatusCode(),
          violation.getDisposition(),
          violation.getOriginalPolicy());
    } catch (JsonProcessingException e) {
      log.warn("CSP violation report could not be parsed, length {}", reportBody.length());
    }
  }
}
