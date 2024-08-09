package uk.gov.laa.ccms.caab.model.sections;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents the Section display values for the application summary screen.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OpponentsSectionDisplay extends ApplicationSectionStatusDisplay {

  /**
   * The list of opponents for the application.
   */
  private List<OpponentSectionDisplay> opponents;

}
