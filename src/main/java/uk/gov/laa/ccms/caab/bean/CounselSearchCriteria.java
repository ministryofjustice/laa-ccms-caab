package uk.gov.laa.ccms.caab.bean;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;

/** Represents the criteria to search for a Counsel. */
@Data
public class CounselSearchCriteria implements Serializable {

  /** The name. */
  @Size(max = 35)
  private String name;

  /** The company. */
  @Size(max = 35)
  private String company;

  /** LAA Counsel Reference. */
  @Size(max = 15)
  private String laaCounselReference;

  /** The Category. */
  @Size(max = 15)
  private String category;
}
