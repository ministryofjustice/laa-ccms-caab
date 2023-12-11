package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/**
 * Represents the client address search form.
 */
@Data
public class ClientFormDataDeceasedDetails {
  private String dodDay;
  private String dodMonth;
  private String dodYear;
  private String dateOfDeath;
}
