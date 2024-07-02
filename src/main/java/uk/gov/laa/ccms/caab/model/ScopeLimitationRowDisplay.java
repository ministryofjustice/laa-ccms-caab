package uk.gov.laa.ccms.caab.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.util.Pair;

/**
 * Represents the Summary display values for a Scope Limitation.
 */
@Data
@Builder
public class ScopeLimitationRowDisplay {

  private String scopeLimitation;

  private String wording;


}
