package uk.gov.laa.ccms.caab.bean;

import java.util.List;
import lombok.Data;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;

/**
 * Represents the form data for a summary submission.
 * Contains a list of dynamic checkbox options for declarations.
 */
@Data
public class SummarySubmissionFormData {


  /**
   * A map of dynamic options for declarations.
   * The key is the option name and the value is the DynamicCheckbox object.
   */
  private List<DynamicCheckbox> declarationOptions;

}
