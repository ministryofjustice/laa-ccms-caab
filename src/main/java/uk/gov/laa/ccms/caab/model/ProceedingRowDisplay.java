package uk.gov.laa.ccms.caab.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.util.Pair;

/**
 * Represents the Summary display values for a proceeding.
 */
@Data
@Builder
public class ProceedingRowDisplay {

  private String matterType;

  private String levelOfService;

  private String clientInvolvement;

  private String status;

  private List<ScopeLimitationRowDisplay> scopeLimitations;
  

}
