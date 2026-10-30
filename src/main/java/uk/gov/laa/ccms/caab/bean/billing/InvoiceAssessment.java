package uk.gov.laa.ccms.caab.bean.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Retained OPA data for a selected case and billing invoice. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceAssessment(
    String caseReference,
    Long billingIncidentId,
    Long assessmentId,
    String billingProviderId,
    List<Instance> instances) {

  /** A retained entity instance; labels are not necessarily unique. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Instance(Long id, String entityName, List<Attribute> attributes) {}

  /** The raw response is authoritative for numeric totals, not its display text. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Attribute(Long id, String name, String responseValue) {}
}
