package uk.gov.laa.ccms.caab.model.sections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Represents the Section display values for the application summary screen. */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralDetailsSectionDisplay extends ApplicationSectionStatusDisplay {

  /** The status of the application. */
  private String applicationStatus;

  /** The category of law for the application. */
  private String categoryOfLaw;

  /** The preferred correspondence method. */
  private String correspondenceMethod;
}
