package uk.gov.laa.ccms.caab.model.csp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Browser Content Security Policy violation report payload. */
@Data
public class CspReport {

  @JsonProperty("csp-report")
  private Violation cspReport;

  private Violation body;

  public Violation getViolation() {
    return cspReport != null ? cspReport : body;
  }

  /** CSP violation fields from either the legacy report-uri or Reporting API shape. */
  @Data
  public static class Violation {

    @JsonProperty("document-uri")
    private String documentUri;

    @JsonProperty("referrer")
    private String referrer;

    @JsonProperty("violated-directive")
    private String violatedDirective;

    @JsonProperty("effective-directive")
    private String effectiveDirective;

    @JsonProperty("original-policy")
    private String originalPolicy;

    @JsonProperty("blocked-uri")
    private String blockedUri;

    @JsonProperty("source-file")
    private String sourceFile;

    @JsonProperty("line-number")
    private Integer lineNumber;

    @JsonProperty("column-number")
    private Integer columnNumber;

    @JsonProperty("status-code")
    private Integer statusCode;

    @JsonProperty("disposition")
    private String disposition;
  }
}
