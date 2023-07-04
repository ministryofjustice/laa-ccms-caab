package uk.gov.laa.ccms.caab.bean;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationDetails {

  /**
   * The id of the Office related to this Application
   */
  @NotNull(message = "Please select an Office")
  private Integer officeId;

}
