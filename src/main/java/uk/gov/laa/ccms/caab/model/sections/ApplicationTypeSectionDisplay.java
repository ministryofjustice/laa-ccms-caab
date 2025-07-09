package uk.gov.laa.ccms.caab.model.sections;

import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Represents the Section display values for the application summary screen. */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class ApplicationTypeSectionDisplay extends ApplicationSectionStatusDisplay {

  /** The description of the application type. */
  private String description;

  /** Flag to indicate that devolved powers were used in the application. */
  private Boolean devolvedPowersUsed;

  /** Date on which devolved powers were used. */
  private Date devolvedPowersDate;
}
