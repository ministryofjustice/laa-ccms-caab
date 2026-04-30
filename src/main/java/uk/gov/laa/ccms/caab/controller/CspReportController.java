package uk.gov.laa.ccms.caab.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.laa.ccms.caab.bean.metric.PuiMetricService;
import uk.gov.laa.ccms.caab.model.csp.CspReport;

/** Receives browser Content Security Policy violation reports. */
@RestController
@Slf4j
public class CspReportController {

  private final PuiMetricService puiMetricService;
  private final ObjectMapper objectMapper;

  public CspReportController(PuiMetricService puiMetricService, ObjectMapper objectMapper) {
    this.puiMetricService = puiMetricService;
    this.objectMapper = objectMapper;
  }

  /**
   * Receives and logs a browser CSP violation report.
   *
   * @param reportBody raw CSP report body posted by the browser
   * @return an empty 204 response once the report has been recorded
   */
  @PostMapping("/csp/report")
  public ResponseEntity<Void> report(@RequestBody(required = false) String reportBody) {
    puiMetricService.incrementCspViolationReportsCount();
    logReport(reportBody);
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
        log.warn("CSP violation report received without recognised body: {}", reportBody);
        return;
      }

      log.warn(
          "CSP violation: effectiveDirective=[{}], violatedDirective=[{}], blockedUri=[{}], "
              + "documentUri=[{}], sourceFile=[{}], lineNumber=[{}], columnNumber=[{}], "
              + "statusCode=[{}], disposition=[{}], originalPolicy=[{}]",
          violation.getEffectiveDirective(),
          violation.getViolatedDirective(),
          violation.getBlockedUri(),
          violation.getDocumentUri(),
          violation.getSourceFile(),
          violation.getLineNumber(),
          violation.getColumnNumber(),
          violation.getStatusCode(),
          violation.getDisposition(),
          violation.getOriginalPolicy());
    } catch (JsonProcessingException e) {
      log.warn("CSP violation report could not be parsed: {}", reportBody);
    }
  }
}
