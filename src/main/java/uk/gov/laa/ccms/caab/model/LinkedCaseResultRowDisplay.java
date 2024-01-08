package uk.gov.laa.ccms.caab.model;

import lombok.Data;

/**
 * Represents the display data for a linked case result row.
 */
@Data
public class LinkedCaseResultRowDisplay {

  private Integer id;
  private String lscCaseReference;
  private String relationToCase;
  private String clientFirstName;
  private String clientSurname;
  private String clientReferenceNumber;
  private String providerCaseReference;
  private String categoryOfLaw;
  private String feeEarner;
  private String status;

}
