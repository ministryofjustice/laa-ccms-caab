package uk.gov.laa.ccms.caab.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the minimal data to describe an Application.
 */
@Data
@Builder
public class BaseApplication {

  /**
   * The Case Reference Number.
   */
  private String caseReferenceNumber;

  /**
   * The Case Status Display Value.
   */
  private String caseStatus;

  /**
   * The Category Of Law Display Value.
   */
  private String categoryOfLaw;

  /**
   * The Client Reference Number.
   */
  private String clientReferenceNumber;

  /**
   * The Client's First Name.
   */
  private String clientFirstName;

  /**
   * The Client's Surname.
   */
  private String clientSurname;

  /**
   * The name of the Fee Earner.
   */
  private String feeEarnerName;

  /**
   * The Provider Case Reference Number.
   */
  private String providerCaseReferenceNumber;

  public String getClientFullName() {
    return (clientFirstName != null ? clientFirstName + " " : "")
        + (clientSurname != null ? clientSurname : "");
  }

}
