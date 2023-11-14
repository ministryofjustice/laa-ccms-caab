package uk.gov.laa.ccms.caab.bean;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the details of an active case, this will be stored within the session.
 */
@Data
@Builder
public class ActiveCase {
  private String client;
  private String clientReferenceNumber;

  private String caseReferenceNumber;
  private String providerCaseReferenceNumber;
}
