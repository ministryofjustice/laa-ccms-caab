package uk.gov.laa.ccms.caab.model.summary;

import java.util.List;
import lombok.Data;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@Data
public class ProceedingSubmissionSummaryDisplay {

  private String matterType;

  private String proceeding;

  private String clientInvolvementType;

  private String formOfCivilLegalService;

  private String typeOfOrder;

  private List<ScopeLimitationSubmissionSummaryDisplay> scopeLimitations;

}
