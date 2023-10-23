package uk.gov.laa.ccms.caab.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the status display values for the application summary screen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryStatusDisplay {

  private String status;
  private Date lastSaved;
  private String lastSavedBy;
  private boolean enabled;

  /**
   * Constructor taking in its own class used for copying status displays.
   *
   * @param applicationSummaryStatusDisplay the status display to copy.
   */
  public ApplicationSummaryStatusDisplay(
      ApplicationSummaryStatusDisplay applicationSummaryStatusDisplay) {
    this.status = applicationSummaryStatusDisplay.getStatus();
    this.lastSaved = applicationSummaryStatusDisplay.getLastSaved();
    this.lastSavedBy = applicationSummaryStatusDisplay.getLastSavedBy();
    this.enabled = false;
  }

}
