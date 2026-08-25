package uk.gov.laa.ccms.caab.bean;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;

@Data
public class CourtSearchCriteria implements Serializable {

  @Size(max = 35)
  private String courtCode;

  @Size(max = 35)
  private String courtName;
}
