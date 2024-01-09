package uk.gov.laa.ccms.caab.model;

import lombok.Data;

/**
 * Represents the display data for a linked case result row.
 */
@Data
public class LinkedCaseResultRowDisplay {

  /** Unique identifier for the linked case. */
  private Integer id;

  /** This case's reference number. */
  private String lscCaseReference;

  /** Relation of this case to the primary case. */
  private String relationToCase;

  /** First name of the client associated with the case. */
  private String clientFirstName;

  /** Surname of the client associated with the case. */
  private String clientSurname;

  /** Reference number assigned to the client. */
  private String clientReferenceNumber;

  /** Case reference provided by the service provider. */
  private String providerCaseReference;

  /** Category of law pertaining to the case. */
  private String categoryOfLaw;

  /** Individual responsible for handling the case fees. */
  private String feeEarner;

  /** Current status of the case. */
  private String status;

}
