package uk.gov.laa.ccms.caab.bean.evidence;

import lombok.Data;

/**
 * Represents a required evidence document.
 */
@Data
public class DocumentRequired {

  /**
   * The type of required document.
   */
  private String type;

  /**
   * The code for the required document.
   */
  private String code;

  /**
   * The description of the requirement.
   */
  private String description;

}
