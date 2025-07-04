package uk.gov.laa.ccms.caab.model.sections;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Represents the status display values for the application summary screen. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSectionStatusDisplay {

  /** The status of this section. */
  private String status;

  /** The date this section was last saved. */
  private Date lastSaved;

  /** The user who last saved this section. */
  private String lastSavedBy;

  /** Flag to indicate that this section is enabled. */
  private boolean enabled;
}
