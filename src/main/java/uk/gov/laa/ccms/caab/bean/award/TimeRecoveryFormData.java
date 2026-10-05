package uk.gov.laa.ccms.caab.bean.award;

import lombok.Data;

/** Editable fields on the time-related recovery page. */
@Data
public class TimeRecoveryFormData {

  private String triggeringEvent;
  private String effectiveDate;
  private String timeRelatedRecoveryDetails;
}
