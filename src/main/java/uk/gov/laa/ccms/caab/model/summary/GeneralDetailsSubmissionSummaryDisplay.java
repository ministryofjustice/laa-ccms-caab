package uk.gov.laa.ccms.caab.model.summary;

import java.util.Date;
import lombok.Data;

@Data
public class GeneralDetailsSubmissionSummaryDisplay extends AbstractLookupSummaryDisplay{

  private String categoryOfLaw;
  private String applicationType;
  private Date delegatedFunctionsDate;

}
